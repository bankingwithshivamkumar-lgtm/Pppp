package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.model.BgmMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "BgmSynthesizer"

class BgmSynthesizer(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100

        // Scale frequencies in Hz
        private val BHOPALI_SCALE = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25)
        private val SHIVARANJANI_SCALE = doubleArrayOf(261.63, 293.66, 311.13, 392.00, 440.00, 523.25)
        private val YAMAN_SCALE = doubleArrayOf(261.63, 293.66, 329.63, 369.99, 392.00, 440.00, 493.88, 523.25)

        /**
         * Renders devotional ambient flute + tanpura drone PCM samples for [durationSec].
         */
        fun renderPcmSamples(
            mood: BgmMood,
            startSec: Float,
            durationSec: Float,
            volume: Float
        ): ShortArray {
            val count = (durationSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
            val out = ShortArray(count)
            if (mood == BgmMood.MUTE || volume <= 0.001f) return out

            val scale = when (mood) {
                BgmMood.CALM_FLUTE -> BHOPALI_SCALE
                BgmMood.DEEP_EPIC -> SHIVARANJANI_SCALE
                BgmMood.DEVOTIONAL -> YAMAN_SCALE
                BgmMood.MUTE -> BHOPALI_SCALE
            }
            val melodyPattern = intArrayOf(0, 2, 3, 4, 3, 2, 1, 0, 2, 4, 5, 4, 3, 1, 2, 0)
            val noteDurSec = when (mood) {
                BgmMood.CALM_FLUTE -> 1.5
                BgmMood.DEEP_EPIC -> 1.8
                BgmMood.DEVOTIONAL -> 1.25
                BgmMood.MUTE -> 1.5
            }

            val rootSa = if (mood == BgmMood.DEEP_EPIC) 110.0 else 130.81 // A2 or C3
            val pa = rootSa * 1.5

            for (i in 0 until count) {
                val t = startSec.toDouble() + i.toDouble() / SAMPLE_RATE

                // 1. Warm Tanpura drone (Sa + Pa + upper Sa with slow organic breathing)
                val droneLfo = 0.78 + 0.22 * sin(2.0 * PI * 0.22 * t)
                val drone = (
                    0.45 * sin(2.0 * PI * rootSa * t) +
                        0.15 * sin(2.0 * PI * (rootSa * 2.0) * t) +
                        0.28 * sin(2.0 * PI * pa * t) +
                        0.12 * sin(2.0 * PI * (rootSa * 4.0) * t)
                    ) * droneLfo * 0.34

                // 2. Bansuri Flute melody with smooth ADSR + breath vibrato
                val noteIndex = ((t / noteDurSec).toInt()) % melodyPattern.size
                val noteLocalT = t % noteDurSec
                val noteNorm = noteLocalT / noteDurSec
                val env = when {
                    noteNorm < 0.14 -> noteNorm / 0.14
                    noteNorm > 0.75 -> (1.0 - noteNorm) / 0.25
                    else -> 1.0
                }.coerceIn(0.0, 1.0)

                val baseFreq = scale[melodyPattern[noteIndex] % scale.size]
                val vibrato = 1.0 + 0.0055 * sin(2.0 * PI * 5.2 * t)
                val freq = baseFreq * vibrato
                val flute = (
                    0.78 * sin(2.0 * PI * freq * t) +
                        0.18 * sin(2.0 * PI * freq * 2.0 * t) +
                        0.04 * sin(2.0 * PI * freq * 3.0 * t)
                    ) * env * 0.36

                // 3. Mood ornament: Deep Epic soft drum pulse OR Devotional temple bell shimmer
                val ornament = when (mood) {
                    BgmMood.DEEP_EPIC -> {
                        val beatT = t % 2.0
                        if (beatT < 0.45) {
                            val drumFreq = 58.0 * exp(-beatT * 4.5) + 38.0
                            sin(2.0 * PI * drumFreq * beatT) * exp(-beatT * 7.0) * 0.38
                        } else 0.0
                    }
                    BgmMood.DEVOTIONAL -> {
                        val chimeT = t % 3.0
                        if (chimeT < 1.2) {
                            (sin(2.0 * PI * 1046.5 * chimeT) * 0.6 +
                                sin(2.0 * PI * 1567.98 * chimeT) * 0.3) *
                                exp(-chimeT * 3.8) * 0.16
                        } else 0.0
                    }
                    else -> 0.0
                }

                val mixed = ((drone + flute + ornament) * volume.coerceIn(0f, 1f)).coerceIn(-0.98, 0.98)
                out[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
            }
            return out
        }
    }

    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var customPlayer: MediaPlayer? = null

    @Volatile
    private var currentVolume: Float = 0.35f

    @Volatile
    private var currentMood: BgmMood = BgmMood.CALM_FLUTE

    fun updateSettings(mood: BgmMood, volume: Float) {
        currentMood = mood
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            customPlayer?.setVolume(currentVolume, currentVolume)
        } catch (_: Exception) {
        }
    }

    fun start(
        scope: CoroutineScope,
        mood: BgmMood,
        volume: Float,
        customBgmUri: String?,
        startPositionMs: Long = 0L
    ) {
        stop()
        currentMood = mood
        currentVolume = volume.coerceIn(0f, 1f)

        if (!customBgmUri.isNullOrBlank()) {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(customBgmUri))
                    isLooping = true
                    setVolume(currentVolume, currentVolume)
                    prepare()
                    if (startPositionMs > 0 && duration > 0) {
                        seekTo((startPositionMs % duration).toInt())
                    }
                    start()
                }
                customPlayer = player
                return
            } catch (e: Exception) {
                Log.w(TAG, "Custom BGM playback failed, falling back to synthesizer", e)
            }
        }

        if (mood == BgmMood.MUTE) return

        val chunkSamples = SAMPLE_RATE / 4 // 250ms chunks
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(chunkSamples * 2 * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        playbackJob = scope.launch(Dispatchers.Default) {
            var cursorSec = startPositionMs / 1000f
            while (isActive) {
                val samples = renderPcmSamples(
                    mood = currentMood,
                    startSec = cursorSec,
                    durationSec = 0.25f,
                    volume = currentVolume
                )
                cursorSec += 0.25f
                if (isActive) {
                    track.write(samples, 0, samples.size)
                }
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null

        try {
            customPlayer?.stop()
            customPlayer?.release()
        } catch (_: Exception) {
        }
        customPlayer = null
    }
}
