package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ScriptGeneratorRepository
import com.example.audio.BgmSynthesizer
import com.example.audio.TtsStatus
import com.example.audio.VoiceoverEngine
import com.example.exporter.VideoExporter
import com.example.model.AudioSettings
import com.example.model.BgmMood
import com.example.model.CameraShot
import com.example.model.CaptionFrameStyle
import com.example.model.EnvironmentType
import com.example.model.ExportResolution
import com.example.model.ExportState
import com.example.model.SceneItem
import com.example.model.ScriptLength
import com.example.model.ScriptTone
import com.example.model.ScriptTopic
import com.example.model.ScriptUiState
import com.example.model.Speaker
import com.example.model.StudioDefaults
import com.example.model.VisualSettings
import com.example.renderer.MythologicalSceneRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class StudioTab(
    val title: String,
    val hindiLabel: String
) {
    STUDIO("Studio", "स्टूडियो"),
    AI_SCRIPT("AI Script", "स्क्रिप्ट लिखें"),
    VISUALS("Visuals", "दृश्य व कैप्शन"),
    AUDIO_EXPORT("Audio & Export", "आवाज़ व निर्यात")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    val renderer = MythologicalSceneRenderer(application)
    private val scriptRepo = ScriptGeneratorRepository()
    private val bgmSynthesizer = BgmSynthesizer(application)
    private val voiceoverEngine = VoiceoverEngine(application)
    private val videoExporter = VideoExporter(application, renderer, voiceoverEngine)

    val ttsStatus: StateFlow<TtsStatus> = voiceoverEngine.ttsStatus
    val availableVoices = voiceoverEngine.availableVoices
    val defaultVoiceAssignments = voiceoverEngine.defaultVoiceAssignments

    private val _activeTab = MutableStateFlow(StudioTab.STUDIO)
    val activeTab: StateFlow<StudioTab> = _activeTab.asStateFlow()

    private val _scenes = MutableStateFlow(StudioDefaults.starterScenes())
    val scenes: StateFlow<List<SceneItem>> = _scenes.asStateFlow()

    private val _selectedSceneIndex = MutableStateFlow(0)
    val selectedSceneIndex: StateFlow<Int> = _selectedSceneIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTimeSec = MutableStateFlow(0f)
    val currentTimeSec: StateFlow<Float> = _currentTimeSec.asStateFlow()

    private val _visualSettings = MutableStateFlow(VisualSettings())
    val visualSettings: StateFlow<VisualSettings> = _visualSettings.asStateFlow()

    private val _audioSettings = MutableStateFlow(AudioSettings())
    val audioSettings: StateFlow<AudioSettings> = _audioSettings.asStateFlow()

    // AI Script Generator State
    private val _selectedTopic = MutableStateFlow(ScriptTopic.DUTY)
    val selectedTopic: StateFlow<ScriptTopic> = _selectedTopic.asStateFlow()

    private val _customTopic = MutableStateFlow("")
    val customTopic: StateFlow<String> = _customTopic.asStateFlow()

    private val _selectedTone = MutableStateFlow(ScriptTone.INSPIRING)
    val selectedTone: StateFlow<ScriptTone> = _selectedTone.asStateFlow()

    private val _selectedLength = MutableStateFlow(ScriptLength.MEDIUM_30)
    val selectedLength: StateFlow<ScriptLength> = _selectedLength.asStateFlow()

    private val _scriptUiState = MutableStateFlow<ScriptUiState>(ScriptUiState.Idle)
    val scriptUiState: StateFlow<ScriptUiState> = _scriptUiState.asStateFlow()

    private val _draftScenes = MutableStateFlow<List<SceneItem>>(emptyList())
    val draftScenes: StateFlow<List<SceneItem>> = _draftScenes.asStateFlow()

    // Export State
    private val _exportResolution = MutableStateFlow(ExportResolution.HD_720)
    val exportResolution: StateFlow<ExportResolution> = _exportResolution.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null
    private var lastSpokenSceneId: String? = null

    fun selectTab(tab: StudioTab) {
        _activeTab.value = tab
    }

    fun totalDuration(): Float =
        _scenes.value.sumOf { it.durationSec.toDouble() }.toFloat().coerceAtLeast(1f)

    fun sceneAtTime(timeSec: Float): Triple<Int, SceneItem, Float> {
        val list = _scenes.value
        if (list.isEmpty()) {
            return Triple(0, SceneItem(dialogue = ""), 0f)
        }
        var acc = 0f
        list.forEachIndexed { idx, scene ->
            val end = acc + scene.durationSec
            if (timeSec < end || idx == list.lastIndex) {
                val prog = ((timeSec - acc) / scene.durationSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                return Triple(idx, scene, prog)
            }
            acc = end
        }
        return Triple(0, list.first(), 0f)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun play() {
        if (_scenes.value.isEmpty()) return
        if (_currentTimeSec.value >= totalDuration() - 0.05f) {
            _currentTimeSec.value = 0f
        }
        _isPlaying.value = true
        lastSpokenSceneId = null

        val audio = _audioSettings.value
        val startMs = (_currentTimeSec.value * 1000L).toLong()
        bgmSynthesizer.start(
            scope = viewModelScope,
            mood = audio.bgmMood,
            volume = audio.bgmVolume,
            customBgmUri = audio.customBgmUri,
            startPositionMs = startMs
        )

        if (!audio.fullVideoVoiceoverUri.isNullOrBlank()) {
            voiceoverEngine.startFullVideoVoiceover(
                uriString = audio.fullVideoVoiceoverUri,
                positionMs = startMs,
                volume = audio.voiceoverVolume
            )
        }

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastTickMs = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                val now = System.currentTimeMillis()
                val dt = ((now - lastTickMs) / 1000f).coerceIn(0f, 0.1f)
                lastTickMs = now

                val total = totalDuration()
                val nextTime = _currentTimeSec.value + dt
                if (nextTime >= total) {
                    _currentTimeSec.value = total
                    pause()
                    break
                }
                _currentTimeSec.value = nextTime

                val (idx, scene, _) = sceneAtTime(nextTime)
                if (_selectedSceneIndex.value != idx) {
                    _selectedSceneIndex.value = idx
                }
                if (lastSpokenSceneId != scene.id) {
                    lastSpokenSceneId = scene.id
                    voiceoverEngine.speakScene(scene, _audioSettings.value)
                }
                delay(33L)
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        lastSpokenSceneId = null
        bgmSynthesizer.stop()
        voiceoverEngine.stopAll()
    }

    fun restart() {
        _currentTimeSec.value = 0f
        _selectedSceneIndex.value = 0
        lastSpokenSceneId = null
        if (_isPlaying.value) {
            play()
        }
    }

    fun seekTo(timeSec: Float) {
        val clamped = timeSec.coerceIn(0f, totalDuration())
        _currentTimeSec.value = clamped
        val (idx, _, _) = sceneAtTime(clamped)
        _selectedSceneIndex.value = idx
        if (_isPlaying.value) {
            lastSpokenSceneId = null
            val audio = _audioSettings.value
            if (!audio.fullVideoVoiceoverUri.isNullOrBlank()) {
                voiceoverEngine.startFullVideoVoiceover(
                    uriString = audio.fullVideoVoiceoverUri,
                    positionMs = (clamped * 1000L).toLong(),
                    volume = audio.voiceoverVolume
                )
            }
        }
    }

    fun selectAndSeekScene(index: Int) {
        val list = _scenes.value
        if (index !in list.indices) return
        _selectedSceneIndex.value = index
        var acc = 0f
        for (i in 0 until index) {
            acc += list[i].durationSec
        }
        _currentTimeSec.value = acc
        if (_isPlaying.value) {
            lastSpokenSceneId = null
        }
    }

    // Scene Timeline Edits
    fun addScene() {
        val nextSpeaker = when (_scenes.value.lastOrNull()?.speaker) {
            Speaker.RAM -> Speaker.LAKSHMAN
            Speaker.LAKSHMAN -> Speaker.HANUMAN
            else -> Speaker.RAM
        }
        val newScene = SceneItem(
            speaker = nextSpeaker,
            dialogue = "सत्य और धैर्य का मार्ग ही परम कल्याण की ओर ले जाता है।",
            cameraShot = CameraShot.TWO_SHOT,
            environment = _scenes.value.lastOrNull()?.environment ?: EnvironmentType.DUSK_CAMP,
            durationSec = 5f
        )
        _scenes.update { it + newScene }
        selectAndSeekScene(_scenes.value.lastIndex)
    }

    fun deleteScene(sceneId: String) {
        if (_scenes.value.size <= 1) return
        _scenes.update { list -> list.filterNot { it.id == sceneId } }
        val safeIdx = _selectedSceneIndex.value.coerceIn(0, _scenes.value.lastIndex)
        selectAndSeekScene(safeIdx)
    }

    fun moveScene(index: Int, delta: Int) {
        val list = _scenes.value.toMutableList()
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        _scenes.value = list
        selectAndSeekScene(target)
    }

    fun updateScene(sceneId: String, transform: (SceneItem) -> SceneItem) {
        _scenes.update { list ->
            list.map { if (it.id == sceneId) transform(it) else it }
        }
    }

    // Visual Settings
    fun updateVisualSettings(transform: (VisualSettings) -> VisualSettings) {
        _visualSettings.update(transform)
    }

    fun setCustomCharacterImage(speaker: Speaker, uri: Uri?) {
        takePersistableUriPermission(uri)
        val str = uri?.toString()
        _visualSettings.update { vs ->
            when (speaker) {
                Speaker.RAM -> vs.copy(customRamImageUri = str)
                Speaker.LAKSHMAN -> vs.copy(customLakshmanImageUri = str)
                Speaker.HANUMAN -> vs.copy(customHanumanImageUri = str)
            }
        }
    }

    fun setCustomSceneImage(sceneId: String, uri: Uri?) {
        takePersistableUriPermission(uri)
        updateScene(sceneId) { it.copy(customSceneImageUri = uri?.toString()) }
    }

    fun setCustomSceneVoiceover(sceneId: String, uri: Uri?) {
        takePersistableUriPermission(uri)
        updateScene(sceneId) { it.copy(customVoiceoverUri = uri?.toString()) }
    }

    // Audio Settings
    fun updateAudioSettings(transform: (AudioSettings) -> AudioSettings) {
        _audioSettings.update(transform)
        val latest = _audioSettings.value
        bgmSynthesizer.updateSettings(latest.bgmMood, latest.bgmVolume)
        voiceoverEngine.updateVolume(latest.voiceoverVolume)
        if (_isPlaying.value && latest.bgmMood == BgmMood.MUTE && latest.customBgmUri == null) {
            bgmSynthesizer.stop()
        }
    }

    fun setCustomBgm(uri: Uri?) {
        takePersistableUriPermission(uri)
        updateAudioSettings { it.copy(customBgmUri = uri?.toString()) }
        if (_isPlaying.value) {
            val audio = _audioSettings.value
            bgmSynthesizer.start(
                scope = viewModelScope,
                mood = audio.bgmMood,
                volume = audio.bgmVolume,
                customBgmUri = audio.customBgmUri,
                startPositionMs = (_currentTimeSec.value * 1000L).toLong()
            )
        }
    }

    fun setFullVideoVoiceover(uri: Uri?) {
        takePersistableUriPermission(uri)
        updateAudioSettings { it.copy(fullVideoVoiceoverUri = uri?.toString()) }
    }

    fun testVoiceover(speaker: Speaker) {
        val sampleLine = when (speaker) {
            Speaker.RAM -> "धर्म और धैर्य ही मनुष्य का सबसे बड़ा बल है।"
            Speaker.LAKSHMAN -> "हे प्रभु, आपकी आज्ञा ही मेरा कर्तव्य है।"
            Speaker.HANUMAN -> "जय श्री राम! भक्ति और साहस से सब संभव है।"
        }
        voiceoverEngine.speakScene(
            SceneItem(speaker = speaker, dialogue = sampleLine),
            _audioSettings.value
        )
    }

    // AI Script Generator
    fun setTopic(topic: ScriptTopic) {
        _selectedTopic.value = topic
    }

    fun setCustomTopic(text: String) {
        _customTopic.value = text
    }

    fun setTone(tone: ScriptTone) {
        _selectedTone.value = tone
    }

    fun setLength(length: ScriptLength) {
        _selectedLength.value = length
    }

    fun generateScript() {
        viewModelScope.launch {
            _scriptUiState.value = ScriptUiState.Loading
            val res = scriptRepo.generateSamvadScript(
                topic = _selectedTopic.value,
                customTopic = _customTopic.value,
                tone = _selectedTone.value,
                length = _selectedLength.value
            )
            res.fold(
                onSuccess = { scenes ->
                    _draftScenes.value = scenes
                    _scriptUiState.value = ScriptUiState.Success(scenes)
                },
                onFailure = { err ->
                    _scriptUiState.value = ScriptUiState.Error(
                        err.localizedMessage ?: "Failed to generate script."
                    )
                }
            )
        }
    }

    fun updateDraftScene(sceneId: String, transform: (SceneItem) -> SceneItem) {
        _draftScenes.update { list -> list.map { if (it.id == sceneId) transform(it) else it } }
    }

    fun addDraftScene() {
        val newScene = SceneItem(
            speaker = Speaker.RAM,
            dialogue = "सत्य के पथ पर चलने वाला कभी पराजित नहीं होता।",
            cameraShot = CameraShot.CLOSE_UP,
            environment = EnvironmentType.GOLDEN_ASHRAM,
            durationSec = 5f
        )
        _draftScenes.update { it + newScene }
    }

    fun removeDraftScene(sceneId: String) {
        if (_draftScenes.value.size <= 1) return
        _draftScenes.update { list -> list.filterNot { it.id == sceneId } }
    }

    fun applyDraftToStudio() {
        val draft = _draftScenes.value
        if (draft.isEmpty()) return
        pause()
        _scenes.value = draft
        _selectedSceneIndex.value = 0
        _currentTimeSec.value = 0f
        _activeTab.value = StudioTab.STUDIO
    }

    // Video Export
    fun setExportResolution(res: ExportResolution) {
        _exportResolution.value = res
    }

    fun startExport() {
        if (_exportState.value is ExportState.Rendering) return
        pause()
        exportJob = viewModelScope.launch {
            _exportState.value = ExportState.Rendering(0.02f, "Preparing scenes...")
            val result = videoExporter.exportMp4(
                scenes = _scenes.value,
                visualSettings = _visualSettings.value,
                audioSettings = _audioSettings.value,
                resolution = _exportResolution.value,
                onProgress = { prog, label ->
                    _exportState.value = ExportState.Rendering(prog, label)
                }
            )
            result.fold(
                onSuccess = { out ->
                    _exportState.value = ExportState.Success(
                        uriString = out.uri.toString(),
                        displayName = out.displayName
                    )
                },
                onFailure = { err ->
                    _exportState.value = ExportState.Error(
                        err.localizedMessage ?: "Video export failed."
                    )
                }
            )
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportState.Idle
    }

    fun dismissExportState() {
        _exportState.value = ExportState.Idle
    }

    private fun takePersistableUriPermission(uri: Uri?) {
        if (uri == null) return
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Photo Picker / some providers grant temporary permission without persistable flag
        }
    }

    override fun onCleared() {
        super.onCleared()
        pause()
        voiceoverEngine.release()
    }
}
