package com.example

import com.example.audio.BgmSynthesizer
import com.example.model.BgmMood
import com.example.model.CameraShot
import com.example.model.EnvironmentType
import com.example.model.Speaker
import com.example.model.StudioDefaults
import com.example.model.splitIntoCaptionPhrases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun starterScenesArePopulated() {
        val scenes = StudioDefaults.starterScenes()
        assertEquals(4, scenes.size)
        assertTrue(scenes.all { it.dialogue.isNotBlank() && it.durationSec >= 3f })
    }

    @Test
    fun captionPhrasesSplitCleanly() {
        val phrases = splitIntoCaptionPhrases(
            "हे लक्ष्मण! कठिन समय हमें तोड़ने नहीं, हमारे भीतर के धर्म और साहस को तराशने आता है।"
        )
        assertTrue(phrases.size >= 2)
        assertTrue(phrases.all { it.isNotBlank() })
    }

    @Test
    fun enumParsingHandlesIdsAndFallbacks() {
        assertEquals(Speaker.HANUMAN, Speaker.fromId("Hanuman"))
        assertEquals(CameraShot.OVER_SHOULDER, CameraShot.fromId("over-shoulder"))
        assertEquals(EnvironmentType.GOLDEN_ASHRAM, EnvironmentType.fromId("golden-ashram"))
    }

    @Test
    fun bgmSynthesizerProducesNonZeroPcmForActiveMood() {
        val samples = BgmSynthesizer.renderPcmSamples(
            mood = BgmMood.CALM_FLUTE,
            startSec = 0f,
            durationSec = 0.5f,
            volume = 0.8f
        )
        assertEquals(BgmSynthesizer.SAMPLE_RATE / 2, samples.size)
        assertTrue(samples.any { it != 0.toShort() })
    }

    @Test
    fun characterVoiceDspProducesDistinctWaveformsPerSpeaker() {
        val input = ShortArray(4410) { i -> ((i % 80) * 200 - 8000).toShort() }
        val ram = com.example.audio.CharacterVoiceDsp.processPcm(input, 44100, Speaker.RAM)
        val lakshman = com.example.audio.CharacterVoiceDsp.processPcm(input, 44100, Speaker.LAKSHMAN)
        val hanuman = com.example.audio.CharacterVoiceDsp.processPcm(input, 44100, Speaker.HANUMAN)
        assertEquals(input.size, ram.size)
        assertTrue(!ram.contentEquals(lakshman))
        assertTrue(!hanuman.contentEquals(ram))
    }
}
