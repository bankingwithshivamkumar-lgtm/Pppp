package com.example.audio

import com.example.model.Speaker
import kotlin.math.roundToInt

/**
 * Applies character-specific acoustic signatures to 16-bit mono PCM audio
 * so Shri Ram, Lakshman, and Hanuman each sound distinctly different even when
 * sharing an underlying TTS voice engine.
 */
object CharacterVoiceDsp {

    fun processPcm(
        samples: ShortArray,
        sampleRate: Int,
        speaker: Speaker
    ): ShortArray {
        if (samples.isEmpty()) return samples

        return when (speaker) {
            Speaker.RAM -> applyDivineWarmReverb(samples, sampleRate)
            Speaker.LAKSHMAN -> applyCrispWarriorPresence(samples)
            Speaker.HANUMAN -> applyDeepMightyResonance(samples, sampleRate)
        }
    }

    /**
     * Shri Ram: Warm low-mid smoothness + serene temple early reflections.
     */
    private fun applyDivineWarmReverb(input: ShortArray, sampleRate: Int): ShortArray {
        val out = ShortArray(input.size)
        val delay1 = (0.042f * sampleRate).roundToInt().coerceAtLeast(1)
        val delay2 = (0.089f * sampleRate).roundToInt().coerceAtLeast(1)
        var lp = 0f

        for (i in input.indices) {
            val dry = input[i].toFloat()
            lp = lp * 0.68f + dry * 0.32f
            val warm = dry * 0.72f + lp * 0.34f
            val e1 = if (i >= delay1) input[i - delay1] * 0.16f else 0f
            val e2 = if (i >= delay2) input[i - delay2] * 0.09f else 0f
            val mixed = warm + e1 + e2
            out[i] = mixed.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /**
     * Lakshman: Bright crisp articulation & energetic forward presence.
     */
    private fun applyCrispWarriorPresence(input: ShortArray): ShortArray {
        val out = ShortArray(input.size)
        var prev = 0f
        for (i in input.indices) {
            val dry = input[i].toFloat()
            val highPass = dry - prev
            prev = dry
            val boosted = dry * 1.05f + highPass * 0.28f
            out[i] = boosted.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /**
     * Hanuman: Deep bass weight, rich chest resonance, and monumental cavern echo.
     */
    private fun applyDeepMightyResonance(input: ShortArray, sampleRate: Int): ShortArray {
        val out = ShortArray(input.size)
        val delay1 = (0.058f * sampleRate).roundToInt().coerceAtLeast(1)
        val delay2 = (0.115f * sampleRate).roundToInt().coerceAtLeast(1)

        // Two-stage low-pass for deep chest warmth + gentle sub-bass reinforcement
        var lp1 = 0f
        var lp2 = 0f
        for (i in input.indices) {
            val dry = input[i].toFloat()
            lp1 = lp1 * 0.78f + dry * 0.22f
            lp2 = lp2 * 0.85f + lp1 * 0.15f
            val deepBody = dry * 0.65f + lp2 * 0.65f
            val e1 = if (i >= delay1) input[i - delay1] * 0.22f else 0f
            val e2 = if (i >= delay2) input[i - delay2] * 0.12f else 0f
            val mixed = (deepBody + e1 + e2) * 1.08f
            out[i] = mixed.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
