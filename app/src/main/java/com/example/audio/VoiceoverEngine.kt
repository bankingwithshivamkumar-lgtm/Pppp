package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.model.AudioSettings
import com.example.model.SceneItem
import com.example.model.Speaker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "VoiceoverEngine"

enum class TtsStatus(val label: String, val isReady: Boolean) {
    INITIALIZING("Initializing Hindi TTS...", false),
    READY_HINDI("Hindi (hi-IN) Ready", true),
    READY_FALLBACK("Default Voice Ready (hi-IN pack missing)", true),
    UNAVAILABLE("TTS Unavailable — Use Custom Audio", false)
}

data class TtsVoiceOption(
    val name: String,
    val displayLabel: String,
    val localeTag: String,
    val isOffline: Boolean
)

class VoiceoverEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var sceneAudioPlayer: MediaPlayer? = null
    private var fullAudioPlayer: MediaPlayer? = null
    private var fxAudioTrack: AudioTrack? = null
    private var speakJob: Job? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _ttsStatus = MutableStateFlow(TtsStatus.INITIALIZING)
    val ttsStatus: StateFlow<TtsStatus> = _ttsStatus.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceOption>> = _availableVoices.asStateFlow()

    private val _defaultVoiceAssignments = MutableStateFlow<Map<Speaker, String>>(emptyMap())
    val defaultVoiceAssignments: StateFlow<Map<Speaker, String>> = _defaultVoiceAssignments.asStateFlow()

    private val voiceMap = ConcurrentHashMap<String, Voice>()
    private val pendingCompletions = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    @Volatile
    private var currentVoiceVolume: Float = 1.0f

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to construct TextToSpeech", e)
            _ttsStatus.value = TtsStatus.UNAVAILABLE
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _ttsStatus.value = TtsStatus.UNAVAILABLE
            return
        }
        val engine = tts ?: return
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                utteranceId?.let { pendingCompletions.remove(it)?.complete(true) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                utteranceId?.let { pendingCompletions.remove(it)?.complete(false) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId?.let { pendingCompletions.remove(it)?.complete(false) }
            }
        })

        val hindi = Locale("hi", "IN")
        val res = engine.setLanguage(hindi)
        _ttsStatus.value = when (res) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> TtsStatus.READY_HINDI
            else -> {
                Log.w(TAG, "hi-IN TTS not available (code=$res), using default locale fallback")
                TtsStatus.READY_FALLBACK
            }
        }

        discoverAndAssignCharacterVoices(engine)
    }

    private fun discoverAndAssignCharacterVoices(engine: TextToSpeech) {
        val allVoices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        if (allVoices.isEmpty()) return

        voiceMap.clear()
        allVoices.forEach { voiceMap[it.name] = it }

        // Prioritize Hindi voices (offline/local first), then Indian English / device default voices
        val hindiVoices = allVoices.filter {
            it.locale.language.equals("hi", ignoreCase = true)
        }.sortedWith(
            compareBy<Voice> { it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
                .thenBy { it.name }
        )

        val secondaryVoices = allVoices.filter {
            !it.locale.language.equals("hi", ignoreCase = true) &&
                (it.locale.country.equals("IN", ignoreCase = true) ||
                    it.locale == Locale.getDefault())
        }.sortedWith(
            compareBy<Voice> { it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
        )

        val candidatePool = (hindiVoices + secondaryVoices).ifEmpty { allVoices }
            .distinctBy { it.name }

        val options = candidatePool.take(12).mapIndexed { idx, v ->
            val tag = v.locale.toLanguageTag()
            val shortName = v.name
                .removePrefix("hi-in-x-")
                .removePrefix("en-in-x-")
                .removePrefix("en-us-x-")
                .take(12)
            val mode = if (v.isNetworkConnectionRequired) "Net" else "Local"
            TtsVoiceOption(
                name = v.name,
                displayLabel = "V${idx + 1}: $shortName ($tag, $mode)",
                localeTag = tag,
                isOffline = !v.isNetworkConnectionRequired
            )
        }
        _availableVoices.value = options

        // Auto-assign 3 distinct default voices across Ram, Lakshman, and Hanuman
        val distinctPool = if (hindiVoices.isNotEmpty()) hindiVoices else candidatePool
        val assignments = mapOf(
            Speaker.RAM to distinctPool[0].name,
            Speaker.LAKSHMAN to distinctPool[minOf(1, distinctPool.lastIndex)].name,
            Speaker.HANUMAN to distinctPool[minOf(2, distinctPool.lastIndex)].name
        )
        _defaultVoiceAssignments.value = assignments
        Log.d(TAG, "Assigned default character voices: $assignments")
    }

    private fun resolveVoiceFor(speaker: Speaker, settings: AudioSettings): Voice? {
        val preferredName = settings.voiceNameFor(speaker)
            ?: _defaultVoiceAssignments.value[speaker]
        return preferredName?.let { voiceMap[it] }
    }

    private fun applySpeakerParameters(engine: TextToSpeech, speaker: Speaker, settings: AudioSettings) {
        resolveVoiceFor(speaker, settings)?.let { voice ->
            runCatching { engine.voice = voice }
        }
        engine.setPitch(settings.pitchFor(speaker))
        engine.setSpeechRate(settings.rateFor(speaker))
    }

    fun startFullVideoVoiceover(uriString: String, positionMs: Long, volume: Float) {
        stopFullVideoVoiceover()
        currentVoiceVolume = volume.coerceIn(0f, 1f)
        try {
            val player = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uriString))
                setVolume(currentVoiceVolume, currentVoiceVolume)
                prepare()
                if (positionMs > 0 && positionMs < duration) {
                    seekTo(positionMs.toInt())
                }
                start()
            }
            fullAudioPlayer = player
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play full video voiceover", e)
        }
    }

    fun updateVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        currentVoiceVolume = v
        try {
            sceneAudioPlayer?.setVolume(v, v)
            fullAudioPlayer?.setVolume(v, v)
            fxAudioTrack?.setVolume(v)
        } catch (_: Exception) {
        }
    }

    fun speakScene(scene: SceneItem, settings: AudioSettings) {
        stopSceneVoiceover()

        if (!settings.fullVideoVoiceoverUri.isNullOrBlank()) return

        val vol = settings.voiceoverVolume.coerceIn(0f, 1f)
        currentVoiceVolume = vol
        if (vol <= 0.01f) return

        // Custom per-scene audio takes precedence
        val customUri = scene.customVoiceoverUri
        if (!customUri.isNullOrBlank()) {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(customUri))
                    setVolume(vol, vol)
                    prepare()
                    start()
                }
                sceneAudioPlayer = player
                return
            } catch (e: Exception) {
                Log.w(TAG, "Custom scene voiceover failed, falling back to TTS", e)
            }
        }

        val engine = tts ?: return
        if (!_ttsStatus.value.isReady) return
        if (scene.dialogue.isBlank()) return

        if (settings.enableAcousticFx) {
            speakJob = engineScope.launch {
                val cacheDir = File(context.cacheDir, "tts_fx").apply { mkdirs() }
                val wavFile = File(cacheDir, "preview_${scene.speaker.id.lowercase()}.wav")
                val synthesized = synthesizeSceneToWav(scene, settings, wavFile)
                if (!isActive) return@launch

                if (synthesized) {
                    val decoded = decodeWavMono(wavFile)
                    if (decoded != null && isActive) {
                        val (pcm, rate) = decoded
                        val processed = CharacterVoiceDsp.processPcm(pcm, rate, scene.speaker)
                        playPcmThroughAudioTrack(processed, rate)
                        return@launch
                    }
                }
                // Seamless fallback to direct TTS speak if synthesis isn't supported
                if (isActive) {
                    speakDirectTts(engine, scene, settings, vol)
                }
            }
        } else {
            speakDirectTts(engine, scene, settings, vol)
        }
    }

    private fun speakDirectTts(
        engine: TextToSpeech,
        scene: SceneItem,
        settings: AudioSettings,
        volume: Float
    ) {
        applySpeakerParameters(engine, scene.speaker, settings)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        engine.speak(
            scene.dialogue,
            TextToSpeech.QUEUE_FLUSH,
            params,
            "scene_${scene.id}_${System.currentTimeMillis()}"
        )
    }

    private fun playPcmThroughAudioTrack(pcm: ShortArray, sampleRate: Int) {
        if (pcm.isEmpty()) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 4)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        fxAudioTrack = track
        track.setVolume(currentVoiceVolume)
        track.play()

        var offset = 0
        val chunk = 2048
        while (offset < pcm.size && fxAudioTrack === track) {
            val count = minOf(chunk, pcm.size - offset)
            val written = track.write(pcm, offset, count)
            if (written <= 0) break
            offset += written
        }
    }

    suspend fun synthesizeSceneToWav(
        scene: SceneItem,
        settings: AudioSettings,
        outFile: File
    ): Boolean {
        val engine = tts ?: return false
        if (!_ttsStatus.value.isReady || scene.dialogue.isBlank()) return false

        val utteranceId = "export_${UUID.randomUUID()}"
        val deferred = CompletableDeferred<Boolean>()
        pendingCompletions[utteranceId] = deferred

        applySpeakerParameters(engine, scene.speaker, settings)

        val result = engine.synthesizeToFile(scene.dialogue, Bundle(), outFile, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            pendingCompletions.remove(utteranceId)
            return false
        }

        val ok = withTimeoutOrNull(8_000L) { deferred.await() } ?: false
        pendingCompletions.remove(utteranceId)
        return ok && outFile.exists() && outFile.length() > 44
    }

    private fun decodeWavMono(wavFile: File): Pair<ShortArray, Int>? {
        return try {
            val bytes = wavFile.readBytes()
            if (bytes.size <= 44) return null
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val channels = bb.getShort(22).toInt().coerceAtLeast(1)
            val sampleRate = bb.getInt(24).coerceAtLeast(8000)

            var dataOffset = 12
            var dataSize = 0
            while (dataOffset + 8 < bytes.size) {
                val id = String(bytes, dataOffset, 4, Charsets.US_ASCII)
                val chunkLen = bb.getInt(dataOffset + 4)
                if (id == "data") {
                    dataOffset += 8
                    dataSize = minOf(chunkLen, bytes.size - dataOffset)
                    break
                }
                dataOffset += 8 + chunkLen
            }
            if (dataSize <= 0) return null

            val totalSamples = dataSize / (2 * channels)
            val mono = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                var sum = 0
                for (ch in 0 until channels) {
                    sum += bb.getShort(dataOffset + (i * channels + ch) * 2).toInt()
                }
                mono[i] = (sum / channels).toShort()
            }
            mono to sampleRate
        } catch (_: Exception) {
            null
        }
    }

    fun stopSceneVoiceover() {
        speakJob?.cancel()
        speakJob = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        try {
            val track = fxAudioTrack
            fxAudioTrack = null
            track?.pause()
            track?.flush()
            track?.release()
        } catch (_: Exception) {
        }
        try {
            sceneAudioPlayer?.stop()
            sceneAudioPlayer?.release()
        } catch (_: Exception) {
        }
        sceneAudioPlayer = null
    }

    fun stopFullVideoVoiceover() {
        try {
            fullAudioPlayer?.stop()
            fullAudioPlayer?.release()
        } catch (_: Exception) {
        }
        fullAudioPlayer = null
    }

    fun stopAll() {
        stopSceneVoiceover()
        stopFullVideoVoiceover()
    }

    fun release() {
        stopAll()
        engineScope.cancel()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }
}
