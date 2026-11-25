package com.example.model

import java.util.UUID

enum class Speaker(
    val id: String,
    val englishName: String,
    val hindiName: String,
    val defaultPitch: Float,
    val defaultRate: Float,
    val voiceProfileLabel: String,
    val auraColorArgb: Int,
    val badgeColorHex: Long
) {
    RAM(
        id = "Ram",
        englishName = "Shri Ram",
        hindiName = "श्री राम",
        defaultPitch = 0.88f,
        defaultRate = 0.86f,
        voiceProfileLabel = "Serene & Divine Warmth",
        auraColorArgb = 0xFFFFB300.toInt(),
        badgeColorHex = 0xFF60A5FA
    ),
    LAKSHMAN(
        id = "Lakshman",
        englishName = "Lakshman",
        hindiName = "लक्ष्मण",
        defaultPitch = 1.12f,
        defaultRate = 0.98f,
        voiceProfileLabel = "Crisp & Spirited Warrior",
        auraColorArgb = 0xFF34D399.toInt(),
        badgeColorHex = 0xFF34D399
    ),
    HANUMAN(
        id = "Hanuman",
        englishName = "Hanuman",
        hindiName = "हनुमान",
        defaultPitch = 0.68f,
        defaultRate = 0.82f,
        voiceProfileLabel = "Deep & Mighty Resonance",
        auraColorArgb = 0xFFF59E0B.toInt(),
        badgeColorHex = 0xFFF59E0B
    );

    companion object {
        fun fromId(raw: String): Speaker {
            return entries.firstOrNull {
                it.id.equals(raw.trim(), ignoreCase = true) ||
                    it.englishName.equals(raw.trim(), ignoreCase = true) ||
                    it.hindiName == raw.trim()
            } ?: RAM
        }
    }
}

enum class CameraShot(
    val id: String,
    val englishLabel: String,
    val hindiLabel: String
) {
    TWO_SHOT("two-shot", "Two-Shot", "द्वि-पात्र"),
    CLOSE_UP("close-up", "Close-Up", "समीप दृश्य"),
    WIDE_CAMP("wide-camp", "Wide Camp", "विस्तृत दृश्य"),
    OVER_SHOULDER("over-shoulder", "Over Shoulder", "स्कंध-दृष्टि");

    companion object {
        fun fromId(raw: String): CameraShot {
            return entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) } ?: TWO_SHOT
        }
    }
}

enum class EnvironmentType(
    val id: String,
    val englishLabel: String,
    val hindiLabel: String
) {
    DUSK_CAMP("dusk-camp", "Golden Dusk Camp", "संध्या शिविर"),
    NIGHT_FOREST("night-forest", "Deep Night Forest", "रात्रि वन"),
    GOLDEN_ASHRAM("golden-ashram", "Sacred Ashram", "पावन आश्रम");

    companion object {
        fun fromId(raw: String): EnvironmentType {
            return entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) } ?: DUSK_CAMP
        }
    }
}

enum class CaptionFrameStyle(
    val label: String,
    val prefix: String,
    val suffix: String
) {
    BOW("🏹 ✨", "🏹 ✨ • ", " • 🏹"),
    FLAG("🚩 🙏", "🚩 🙏 • ", " • 🚩"),
    PEACOCK("🦚 ✨", "🦚 ✨ • ", " • 🦚"),
    DIYA("🪔 🕉️", "🪔 🕉️ • ", " • 🪔"),
    MINIMAL("Clean", "", "")
}

enum class BgmMood(
    val id: String,
    val englishLabel: String,
    val hindiLabel: String
) {
    CALM_FLUTE("calm-flute", "Calm Flute", "शांत बांसुरी"),
    DEEP_EPIC("deep-epic", "Deep Epic", "गंभीर महाकाव्य"),
    DEVOTIONAL("devotional", "Devotional", "भक्तिमय"),
    MUTE("mute", "Mute", "मौन")
}

enum class ScriptTopic(
    val englishLabel: String,
    val hindiLabel: String,
    val promptHint: String
) {
    DUTY("Duty", "धर्म और कर्तव्य", "Dharma, righteousness, and unwavering duty even in hardship"),
    SELF_BELIEF("Self-Belief", "आत्मविश्वास", "Awakening inner strength and overcoming self-doubt like Hanuman"),
    PATIENCE("Patience", "धैर्य", "Remaining calm, composed, and patient when destiny tests you"),
    COURAGE("Courage in Crisis", "संकट में साहस", "Fearlessness and resolve in the face of crisis and adversity"),
    DECISION("Decision Making", "निर्णय शक्ति", "Clarity of mind and wise decision-making guided by truth"),
    DEVOTION("Devotion", "भक्ति", "Pure devotion, humility, and surrender to the divine"),
    CUSTOM("Custom Topic", "अपना विषय", "Custom user topic")
}

enum class ScriptTone(
    val englishLabel: String,
    val hindiLabel: String
) {
    CALM_PHILOSOPHICAL("Calm & Philosophical", "शांत व गंभीर"),
    INSPIRING("Inspiring", "प्रेरणादायक"),
    EMOTIONAL("Emotional", "भावुक")
}

enum class ScriptLength(
    val seconds: Int,
    val label: String,
    val sceneRange: IntRange
) {
    SHORT_15(15, "15s", 4..4),
    MEDIUM_30(30, "30s", 5..6),
    LONG_45(45, "45s", 6..8)
}

enum class ExportResolution(
    val width: Int,
    val height: Int,
    val label: String
) {
    HD_720(720, 1280, "720p HD (Fast)"),
    FHD_1080(1080, 1920, "1080p Full HD")
}

data class SceneItem(
    val id: String = UUID.randomUUID().toString(),
    val speaker: Speaker = Speaker.RAM,
    val dialogue: String = "",
    val cameraShot: CameraShot = CameraShot.TWO_SHOT,
    val environment: EnvironmentType = EnvironmentType.DUSK_CAMP,
    val durationSec: Float = 5f,
    val customVoiceoverUri: String? = null,
    val customSceneImageUri: String? = null
)

data class VisualSettings(
    val captionFrameStyle: CaptionFrameStyle = CaptionFrameStyle.BOW,
    val captionFontSizeSp: Float = 18f,
    val captionColorArgb: Int = 0xFFFDE68A.toInt(),
    val showWatermark: Boolean = true,
    val watermarkText: String = "@RamayanaSamvad",
    val watermarkOpacity: Float = 0.42f,
    val particleIntensity: Float = 0.75f,
    val customRamImageUri: String? = null,
    val customLakshmanImageUri: String? = null,
    val customHanumanImageUri: String? = null
)

data class AudioSettings(
    val voiceoverVolume: Float = 1.0f,
    val bgmVolume: Float = 0.35f,
    val bgmMood: BgmMood = BgmMood.CALM_FLUTE,
    val customBgmUri: String? = null,
    val fullVideoVoiceoverUri: String? = null,
    val enableAcousticFx: Boolean = true,
    val ramVoiceName: String? = null,
    val lakshmanVoiceName: String? = null,
    val hanumanVoiceName: String? = null,
    val ramPitch: Float = Speaker.RAM.defaultPitch,
    val ramRate: Float = Speaker.RAM.defaultRate,
    val lakshmanPitch: Float = Speaker.LAKSHMAN.defaultPitch,
    val lakshmanRate: Float = Speaker.LAKSHMAN.defaultRate,
    val hanumanPitch: Float = Speaker.HANUMAN.defaultPitch,
    val hanumanRate: Float = Speaker.HANUMAN.defaultRate
) {
    fun pitchFor(speaker: Speaker): Float = when (speaker) {
        Speaker.RAM -> ramPitch
        Speaker.LAKSHMAN -> lakshmanPitch
        Speaker.HANUMAN -> hanumanPitch
    }

    fun rateFor(speaker: Speaker): Float = when (speaker) {
        Speaker.RAM -> ramRate
        Speaker.LAKSHMAN -> lakshmanRate
        Speaker.HANUMAN -> hanumanRate
    }

    fun voiceNameFor(speaker: Speaker): String? = when (speaker) {
        Speaker.RAM -> ramVoiceName
        Speaker.LAKSHMAN -> lakshmanVoiceName
        Speaker.HANUMAN -> hanumanVoiceName
    }
}

sealed interface ScriptUiState {
    data object Idle : ScriptUiState
    data object Loading : ScriptUiState
    data class Success(val scenes: List<SceneItem>) : ScriptUiState
    data class Error(val message: String) : ScriptUiState
}

sealed interface ExportState {
    data object Idle : ExportState
    data class Rendering(val progress: Float, val statusLabel: String) : ExportState
    data class Success(val uriString: String, val displayName: String) : ExportState
    data class Error(val message: String) : ExportState
}

object StudioDefaults {
    val captionColors = listOf(
        0xFFFDE68A.toInt() to "Royal Gold",
        0xFFFDBA74.toInt() to "Warm Saffron",
        0xFFFFFFFF.toInt() to "Pure Ivory",
        0xFF93C5FD.toInt() to "Divine Sky"
    )

    fun starterScenes(): List<SceneItem> = listOf(
        SceneItem(
            speaker = Speaker.LAKSHMAN,
            dialogue = "भैया, जब चारों ओर संकट का अंधकार हो, तब मन का धैर्य कैसे स्थिर रखें?",
            cameraShot = CameraShot.OVER_SHOULDER,
            environment = EnvironmentType.DUSK_CAMP,
            durationSec = 5f
        ),
        SceneItem(
            speaker = Speaker.RAM,
            dialogue = "हे लक्ष्मण! कठिन समय हमें तोड़ने नहीं, हमारे भीतर के धर्म और साहस को तराशने आता है।",
            cameraShot = CameraShot.CLOSE_UP,
            environment = EnvironmentType.DUSK_CAMP,
            durationSec = 5.5f
        ),
        SceneItem(
            speaker = Speaker.HANUMAN,
            dialogue = "प्रभु के स्मरण और कर्तव्य पथ पर अडिग रहने वाले को कोई भी बाधा रोक नहीं सकती।",
            cameraShot = CameraShot.TWO_SHOT,
            environment = EnvironmentType.NIGHT_FOREST,
            durationSec = 5f
        ),
        SceneItem(
            speaker = Speaker.RAM,
            dialogue = "जहाँ सत्य, धैर्य और संकल्प साथ हों — वहाँ विजय स्वयं मार्ग बना लेती है।",
            cameraShot = CameraShot.WIDE_CAMP,
            environment = EnvironmentType.GOLDEN_ASHRAM,
            durationSec = 5.5f
        )
    )
}

/**
 * Splits a Hindi dialogue into short, punchy caption phrases for phrase-by-phrase syncing.
 */
fun splitIntoCaptionPhrases(dialogue: String): List<String> {
    val clean = dialogue.trim()
    if (clean.isEmpty()) return listOf("")

    // First split by Hindi/punctuation boundaries
    val rawClauses = clean.split(Regex("(?<=[।,!?;:—-])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val phrases = mutableListOf<String>()
    for (clause in rawClauses) {
        val words = clause.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size <= 5) {
            phrases.add(clause)
        } else {
            var idx = 0
            while (idx < words.size) {
                val end = minOf(idx + 4, words.size)
                phrases.add(words.subList(idx, end).joinToString(" "))
                idx = end
            }
        }
    }
    return phrases.ifEmpty { listOf(clean) }
}
