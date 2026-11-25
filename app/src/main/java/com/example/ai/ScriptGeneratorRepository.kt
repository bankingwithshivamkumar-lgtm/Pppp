package com.example.ai

import android.util.Log
import com.example.model.CameraShot
import com.example.model.EnvironmentType
import com.example.model.SceneItem
import com.example.model.ScriptLength
import com.example.model.ScriptTone
import com.example.model.ScriptTopic
import com.example.model.Speaker
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "ScriptGeneratorRepo"

class ScriptGeneratorRepository {

    private val scriptSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short Hindi title for the reel"),
            "scenes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "speaker" to Schema.string(
                            description = "Must be exactly one of: Ram, Lakshman, Hanuman"
                        ),
                        "dialogue" to Schema.string(
                            description = "Impactful, concise Hindi dialogue in Devanagari script suitable for short reel captions"
                        ),
                        "cameraShot" to Schema.string(
                            description = "Must be exactly one of: two-shot, close-up, wide-camp, over-shoulder"
                        ),
                        "environment" to Schema.string(
                            description = "Must be exactly one of: dusk-camp, night-forest, golden-ashram"
                        ),
                        "durationSec" to Schema.integer(
                            description = "Recommended scene duration in seconds between 3 and 7"
                        )
                    )
                ),
                description = "Ordered list of 4 to 8 dialogue scenes"
            )
        )
    )

    private val flashPrimaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scriptSchema
                temperature = 0.8f
            }
        )
    }

    private val flashLiteStructuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scriptSchema
                temperature = 0.8f
            }
        )
    }

    private val flashLiteJsonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.8f
            }
        )
    }

    private val flashJsonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.8f
            }
        )
    }

    suspend fun generateSamvadScript(
        topic: ScriptTopic,
        customTopic: String,
        tone: ScriptTone,
        length: ScriptLength
    ): Result<List<SceneItem>> {
        if (topic == ScriptTopic.CUSTOM && customTopic.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Please enter a custom topic (कृपया अपना विषय लिखें)।")
            )
        }

        val resolvedTopic = if (topic == ScriptTopic.CUSTOM) {
            customTopic.trim()
        } else {
            "${topic.hindiLabel} (${topic.englishLabel}) — ${topic.promptHint}"
        }

        val minScenes = length.sceneRange.first
        val maxScenes = length.sceneRange.last

        val prompt = """
            You are a master Hindi devotional & motivational scriptwriter for animated Ramayana vertical Reels/Shorts ("Ramayana Samvad Studio").
            Create a deeply inspiring, cinematic dialogue between Shri Ram, Lakshman, and Hanuman.

            Parameters:
            - Topic / Theme: $resolvedTopic
            - Tone: ${tone.englishLabel} (${tone.hindiLabel})
            - Target Total Video Length: ~${length.seconds} seconds
            - Scene Count: $minScenes to $maxScenes scenes

            Rules:
            1. Every dialogue MUST be written in pure, eloquent Hindi (Devanagari script only, no Hinglish/Romanized Hindi).
            2. Keep each dialogue short, rhythmic, and impactful (10–20 Hindi words per scene) so captions read cleanly in vertical video bars.
            3. Alternate speakers naturally (e.g., Lakshman asks a heartfelt question or shares a dilemma, Hanuman adds devotion/courage, Shri Ram delivers timeless wisdom).
            4. Vary cameraShot ("two-shot", "close-up", "wide-camp", "over-shoulder") and environment ("dusk-camp", "night-forest", "golden-ashram") across scenes for cinematic pacing.
            5. Ensure the sum of durationSec across all scenes is close to ${length.seconds} seconds (each scene between 4 and 6 seconds).

            Return valid JSON strictly matching:
            {
              "title": "Hindi title",
              "scenes": [
                {
                  "speaker": "Ram | Lakshman | Hanuman",
                  "dialogue": "Hindi Devanagari line",
                  "cameraShot": "two-shot | close-up | wide-camp | over-shoulder",
                  "environment": "dusk-camp | night-forest | golden-ashram",
                  "durationSec": 5
                }
              ]
            }
        """.trimIndent()

        var lastError: Exception? = null

        // Rotate across distinct model pools with brief backoff for transient 503 high-demand spikes
        val attempts = listOf(
            Triple("gemini-3.8-flash (schema)", flashPrimaryModel, 0L),
            Triple("gemini-3.5-flash-lite (schema)", flashLiteStructuredModel, 600L),
            Triple("gemini-3.5-flash-lite (json)", flashLiteJsonModel, 1500L),
            Triple("gemini-3.8-flash (json)", flashJsonModel, 2500L)
        )

        for ((label, candidateModel, preDelayMs) in attempts) {
            if (preDelayMs > 0L) {
                delay(preDelayMs)
            }
            try {
                val response = withTimeout(45_000L) {
                    candidateModel.generateContent(prompt)
                }
                val rawText = response.text?.trim()
                    ?: throw IllegalStateException("Gemini returned an empty response ($label).")

                val scenes = parseScenesJson(rawText)
                if (scenes.isNotEmpty()) {
                    Log.d(TAG, "Script generated successfully via $label (${scenes.size} scenes)")
                    return Result.success(scenes)
                } else {
                    lastError = IllegalStateException("Generated script contained no valid scenes ($label).")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Generation timed out on $label", e)
                lastError = Exception("Request timed out ($label). Check internet connectivity and retry.", e)
            } catch (e: Exception) {
                Log.e(TAG, "Generation attempt failed on $label", e)
                lastError = e
                val msg = e.localizedMessage.orEmpty()
                if (msg.contains("403", ignoreCase = true) ||
                    msg.contains("AppCheck", ignoreCase = true) ||
                    msg.contains("UnknownHost", ignoreCase = true)
                ) {
                    break
                }
            }
        }

        val err = lastError ?: Exception("Unknown error generating script.")
        val friendly = formatFriendlyError(err)
        return Result.failure(Exception(friendly, err))
    }

    private fun parseScenesJson(rawText: String): List<SceneItem> {
        val cleaned = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val scenesArray: JSONArray = if (cleaned.startsWith("[")) {
            JSONArray(cleaned)
        } else {
            val startIdx = cleaned.indexOf('{')
            val endIdx = cleaned.lastIndexOf('}')
            val jsonSlice = if (startIdx >= 0 && endIdx > startIdx) {
                cleaned.substring(startIdx, endIdx + 1)
            } else {
                cleaned
            }
            val root = JSONObject(jsonSlice)
            root.optJSONArray("scenes") ?: JSONArray()
        }

        return buildList {
            for (i in 0 until scenesArray.length()) {
                val obj = scenesArray.optJSONObject(i) ?: continue
                val speaker = Speaker.fromId(obj.optString("speaker", "Ram"))
                val dialogue = obj.optString("dialogue", "").trim()
                if (dialogue.isEmpty()) continue
                val shot = CameraShot.fromId(obj.optString("cameraShot", "two-shot"))
                val env = EnvironmentType.fromId(obj.optString("environment", "dusk-camp"))
                val rawDur = obj.optDouble("durationSec", 5.0)
                val dur = rawDur.toFloat().coerceIn(3f, 10f)
                add(
                    SceneItem(
                        speaker = speaker,
                        dialogue = dialogue,
                        cameraShot = shot,
                        environment = env,
                        durationSec = dur
                    )
                )
            }
        }
    }

    private fun formatFriendlyError(e: Exception): String {
        val detail = e.localizedMessage ?: e.javaClass.simpleName
        return when {
            detail.contains("high demand", ignoreCase = true) ||
                detail.contains("503", ignoreCase = true) ||
                detail.contains("overloaded", ignoreCase = true) ||
                detail.contains("unavailable", ignoreCase = true) ->
                "Gemini servers are temporarily busy (high demand). Please wait a few seconds and tap Retry.\n($detail)"
            detail.contains("403", ignoreCase = true) ||
                detail.contains("AppCheck", ignoreCase = true) ||
                detail.contains("attestation", ignoreCase = true) ->
                "Firebase App Check error (403). Please relaunch from AI Studio or check App Check config. Details: $detail"
            detail.contains("UnknownHost", ignoreCase = true) ||
                detail.contains("Unable to resolve host", ignoreCase = true) ||
                detail.contains("ConnectException", ignoreCase = true) ->
                "Network unavailable on emulator/device. Check internet connection and retry. ($detail)"
            else -> "Gemini Script Error: $detail"
        }
    }
}
