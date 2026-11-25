package com.example.exporter

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioFormat
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.audio.BgmSynthesizer
import com.example.audio.VoiceoverEngine
import com.example.model.AudioSettings
import com.example.model.ExportResolution
import com.example.model.SceneItem
import com.example.model.VisualSettings
import com.example.renderer.MythologicalSceneRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext

private const val TAG = "VideoExporter"

data class ExportResult(
    val uri: Uri,
    val displayName: String
)

class VideoExporter(
    private val context: Context,
    private val renderer: MythologicalSceneRenderer,
    private val voiceoverEngine: VoiceoverEngine
) {

    suspend fun exportMp4(
        scenes: List<SceneItem>,
        visualSettings: VisualSettings,
        audioSettings: AudioSettings,
        resolution: ExportResolution,
        onProgress: (Float, String) -> Unit
    ): Result<ExportResult> = withContext(Dispatchers.Default) {
        if (scenes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No scenes to export."))
        }

        val tempDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "SamvadStudio_$timestamp.mp4"
        val tempMp4 = File(tempDir, fileName)
        if (tempMp4.exists()) tempMp4.delete()

        val tempWavs = mutableListOf<File>()

        try {
            val totalDurationSec = scenes.sumOf { it.durationSec.toDouble() }.toFloat().coerceAtLeast(1f)
            val fps = 24
            val totalFrames = (totalDurationSec * fps).toInt().coerceAtLeast(24)
            val width = resolution.width
            val height = resolution.height

            // Step 1: Prepare Audio PCM (44100 Hz Mono)
            onProgress(0.04f, "Synthesizing audio & Hindi voiceover...")
            val totalSamples = (totalDurationSec * BgmSynthesizer.SAMPLE_RATE).toInt().coerceAtLeast(1)

            // 1a. BGM PCM
            val bgmSamples: ShortArray = if (!audioSettings.customBgmUri.isNullOrBlank()) {
                decodeUriToMonoPcm44k(Uri.parse(audioSettings.customBgmUri), totalSamples, loop = true)
                    ?: BgmSynthesizer.renderPcmSamples(
                        mood = audioSettings.bgmMood,
                        startSec = 0f,
                        durationSec = totalDurationSec,
                        volume = 1f
                    )
            } else {
                BgmSynthesizer.renderPcmSamples(
                    mood = audioSettings.bgmMood,
                    startSec = 0f,
                    durationSec = totalDurationSec,
                    volume = 1f
                )
            }

            // 1b. Voiceover PCM
            val voiceSamples = ShortArray(totalSamples)
            if (!audioSettings.fullVideoVoiceoverUri.isNullOrBlank()) {
                val decoded = decodeUriToMonoPcm44k(
                    Uri.parse(audioSettings.fullVideoVoiceoverUri),
                    totalSamples,
                    loop = false
                )
                if (decoded != null) {
                    System.arraycopy(decoded, 0, voiceSamples, 0, minOf(decoded.size, voiceSamples.size))
                }
            } else {
                var sceneStartSec = 0f
                for ((idx, scene) in scenes.withIndex()) {
                    coroutineContext.ensureActive()
                    val startSample = (sceneStartSec * BgmSynthesizer.SAMPLE_RATE).toInt()
                    val maxSceneSamples = (scene.durationSec * BgmSynthesizer.SAMPLE_RATE).toInt()

                    val scenePcm: ShortArray? = if (!scene.customVoiceoverUri.isNullOrBlank()) {
                        decodeUriToMonoPcm44k(
                            Uri.parse(scene.customVoiceoverUri),
                            maxSceneSamples,
                            loop = false
                        )
                    } else {
                        val wavFile = File(tempDir, "tts_scene_${idx}_$timestamp.wav")
                        tempWavs.add(wavFile)
                        val synthesized = voiceoverEngine.synthesizeSceneToWav(scene, audioSettings, wavFile)
                        if (synthesized) {
                            decodeWavToMonoPcm44k(wavFile)?.let { raw ->
                                if (audioSettings.enableAcousticFx) {
                                    com.example.audio.CharacterVoiceDsp.processPcm(
                                        raw,
                                        BgmSynthesizer.SAMPLE_RATE,
                                        scene.speaker
                                    )
                                } else raw
                            }
                        } else null
                    }

                    if (scenePcm != null) {
                        val copyLen = minOf(scenePcm.size, totalSamples - startSample)
                        for (s in 0 until copyLen) {
                            voiceSamples[startSample + s] = scenePcm[s]
                        }
                    }
                    sceneStartSec += scene.durationSec
                }
            }

            // Mix BGM + Voiceover
            val mixedPcm = ShortArray(totalSamples)
            val vVol = audioSettings.voiceoverVolume.coerceIn(0f, 1f)
            val bVol = audioSettings.bgmVolume.coerceIn(0f, 1f)
            for (i in 0 until totalSamples) {
                val sample = voiceSamples[i] * vVol + bgmSamples[i] * bVol
                mixedPcm[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            // Step 2: Configure Video & Audio MediaCodecs + MediaMuxer
            onProgress(0.14f, "Initializing H.264 & AAC encoders...")

            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, if (width >= 1080) 5_000_000 else 3_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoEncoder.start()

            val audioFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                BgmSynthesizer.SAMPLE_RATE,
                1
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            val audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioEncoder.start()

            val muxer = MediaMuxer(tempMp4.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var muxerStarted = false

            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            // Buffer pending encoded packets before muxer starts (until both track formats arrive)
            data class PendingSample(val isVideo: Boolean, val data: ByteBuffer, val info: MediaCodec.BufferInfo)
            val pendingSamples = mutableListOf<PendingSample>()

            fun maybeStartMuxer() {
                if (!muxerStarted && videoTrackIndex >= 0 && audioTrackIndex >= 0) {
                    muxer.start()
                    muxerStarted = true
                    for (item in pendingSamples) {
                        val track = if (item.isVideo) videoTrackIndex else audioTrackIndex
                        muxer.writeSampleData(track, item.data, item.info)
                    }
                    pendingSamples.clear()
                }
            }

            fun drainEncoder(
                encoder: MediaCodec,
                info: MediaCodec.BufferInfo,
                isVideo: Boolean
            ) {
                while (true) {
                    val outIdx = encoder.dequeueOutputBuffer(info, 5_000L)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (isVideo) {
                                videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                            } else {
                                audioTrackIndex = muxer.addTrack(encoder.outputFormat)
                            }
                            maybeStartMuxer()
                        }
                        outIdx >= 0 -> {
                            val encoded = encoder.getOutputBuffer(outIdx)
                            if (encoded != null && info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                encoded.position(info.offset)
                                encoded.limit(info.offset + info.size)
                                if (muxerStarted) {
                                    val track = if (isVideo) videoTrackIndex else audioTrackIndex
                                    muxer.writeSampleData(track, encoded, info)
                                } else {
                                    val copy = ByteBuffer.allocate(info.size)
                                    copy.put(encoded)
                                    copy.flip()
                                    val infoCopy = MediaCodec.BufferInfo().apply {
                                        set(0, info.size, info.presentationTimeUs, info.flags)
                                    }
                                    pendingSamples.add(PendingSample(isVideo, copy, infoCopy))
                                }
                            }
                            encoder.releaseOutputBuffer(outIdx, false)
                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                        }
                    }
                }
            }

            // Step 3: Render & Encode Video Frames
            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            val argbPixels = IntArray(width * height)

            var audioSampleCursor = 0
            val samplesPerChunk = 1024

            for (frameIdx in 0 until totalFrames) {
                coroutineContext.ensureActive()

                val timeSec = frameIdx.toFloat() / fps
                val (scene, sceneProgress) = findSceneAtTime(scenes, timeSec)

                renderer.drawFrame(
                    canvas = frameCanvas,
                    width = width,
                    height = height,
                    scene = scene,
                    sceneProgress = sceneProgress,
                    globalTimeSec = timeSec,
                    totalProgress = timeSec / totalDurationSec,
                    visualSettings = visualSettings,
                    isPlaying = true
                )

                val ptsUs = frameIdx * 1_000_000L / fps
                val inIdx = videoEncoder.dequeueInputBuffer(10_000L)
                if (inIdx >= 0) {
                    val img = try {
                        videoEncoder.getInputImage(inIdx)
                    } catch (_: Exception) {
                        null
                    }
                    if (img != null) {
                        fillYuvImageFromBitmap(img, frameBitmap, argbPixels)
                        val bytesSize = width * height * 3 / 2
                        videoEncoder.queueInputBuffer(inIdx, 0, bytesSize, ptsUs, 0)
                    } else {
                        val buf = videoEncoder.getInputBuffer(inIdx)
                        if (buf != null) {
                            fillNv12BufferFromBitmap(buf, frameBitmap, argbPixels)
                            videoEncoder.queueInputBuffer(inIdx, 0, width * height * 3 / 2, ptsUs, 0)
                        }
                    }
                }
                drainEncoder(videoEncoder, videoBufferInfo, isVideo = true)

                // Interleave AAC audio encoding up to current frame time
                val targetAudioSample = ((timeSec + 0.05f) * BgmSynthesizer.SAMPLE_RATE).toInt()
                    .coerceAtMost(mixedPcm.size)
                while (audioSampleCursor < targetAudioSample) {
                    val aInIdx = audioEncoder.dequeueInputBuffer(5_000L)
                    if (aInIdx < 0) break
                    val aBuf = audioEncoder.getInputBuffer(aInIdx) ?: break
                    aBuf.clear()
                    val chunkCount = minOf(samplesPerChunk, mixedPcm.size - audioSampleCursor)
                    val byteCount = chunkCount * 2
                    val bb = ByteBuffer.allocate(byteCount).order(ByteOrder.LITTLE_ENDIAN)
                    for (k in 0 until chunkCount) {
                        bb.putShort(mixedPcm[audioSampleCursor + k])
                    }
                    bb.flip()
                    aBuf.put(bb)
                    val audioPtsUs = audioSampleCursor * 1_000_000L / BgmSynthesizer.SAMPLE_RATE
                    audioEncoder.queueInputBuffer(aInIdx, 0, byteCount, audioPtsUs, 0)
                    audioSampleCursor += chunkCount
                    drainEncoder(audioEncoder, audioBufferInfo, isVideo = false)
                }

                if (frameIdx % 4 == 0 || frameIdx == totalFrames - 1) {
                    val pct = 0.16f + 0.76f * (frameIdx + 1f) / totalFrames
                    onProgress(pct, "Rendering frame ${frameIdx + 1} / $totalFrames")
                }
            }

            // Flush remaining audio samples
            while (audioSampleCursor < mixedPcm.size) {
                val aInIdx = audioEncoder.dequeueInputBuffer(10_000L)
                if (aInIdx >= 0) {
                    val aBuf = audioEncoder.getInputBuffer(aInIdx)
                    if (aBuf != null) {
                        aBuf.clear()
                        val chunkCount = minOf(samplesPerChunk, mixedPcm.size - audioSampleCursor)
                        val byteCount = chunkCount * 2
                        val bb = ByteBuffer.allocate(byteCount).order(ByteOrder.LITTLE_ENDIAN)
                        for (k in 0 until chunkCount) {
                            bb.putShort(mixedPcm[audioSampleCursor + k])
                        }
                        bb.flip()
                        aBuf.put(bb)
                        val audioPtsUs = audioSampleCursor * 1_000_000L / BgmSynthesizer.SAMPLE_RATE
                        audioEncoder.queueInputBuffer(aInIdx, 0, byteCount, audioPtsUs, 0)
                        audioSampleCursor += chunkCount
                    }
                }
                drainEncoder(audioEncoder, audioBufferInfo, isVideo = false)
            }

            // Signal EOS for both encoders
            val vEosIdx = videoEncoder.dequeueInputBuffer(10_000L)
            if (vEosIdx >= 0) {
                videoEncoder.queueInputBuffer(
                    vEosIdx,
                    0,
                    0,
                    totalFrames * 1_000_000L / fps,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            drainEncoder(videoEncoder, videoBufferInfo, isVideo = true)

            val aEosIdx = audioEncoder.dequeueInputBuffer(10_000L)
            if (aEosIdx >= 0) {
                audioEncoder.queueInputBuffer(
                    aEosIdx,
                    0,
                    0,
                    mixedPcm.size * 1_000_000L / BgmSynthesizer.SAMPLE_RATE,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            drainEncoder(audioEncoder, audioBufferInfo, isVideo = false)

            videoEncoder.stop()
            videoEncoder.release()
            audioEncoder.stop()
            audioEncoder.release()
            frameBitmap.recycle()

            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()

            onProgress(0.95f, "Saving video to Movies/SamvadStudio...")
            val savedUri = publishMp4ToDevice(tempMp4, fileName)
            onProgress(1f, "Export complete!")

            Result.success(ExportResult(uri = savedUri, displayName = fileName))
        } catch (e: Exception) {
            Log.e(TAG, "MP4 export failed", e)
            Result.failure(e)
        } finally {
            tempWavs.forEach { it.delete() }
        }
    }

    private fun publishMp4ToDevice(sourceFile: File, displayName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/SamvadStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    sourceFile
                )

            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            sourceFile.delete()
            uri
        } else {
            val moviesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "SamvadStudio").apply {
                mkdirs()
            }
            val dest = File(moviesDir, displayName)
            sourceFile.copyTo(dest, overwrite = true)
            sourceFile.delete()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
        }
    }

    private fun findSceneAtTime(scenes: List<SceneItem>, timeSec: Float): Pair<SceneItem, Float> {
        var acc = 0f
        for (scene in scenes) {
            val end = acc + scene.durationSec
            if (timeSec < end) {
                val prog = ((timeSec - acc) / scene.durationSec.coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                return scene to prog
            }
            acc = end
        }
        return scenes.last() to 1f
    }

    private fun fillYuvImageFromBitmap(image: Image, bitmap: Bitmap, argb: IntArray) {
        val w = bitmap.width
        val h = bitmap.height
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixStride = yPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val uPixStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixStride = vPlane.pixelStride

        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = argb[y * w + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                val yVal = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
                yBuf.put(y * yRowStride + x * yPixStride, yVal.toByte())

                if ((y and 1) == 0 && (x and 1) == 0) {
                    val uVal = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                    val vVal = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                    val uvY = y shr 1
                    val uvX = x shr 1
                    uBuf.put(uvY * uRowStride + uvX * uPixStride, uVal.toByte())
                    vBuf.put(uvY * vRowStride + uvX * vPixStride, vVal.toByte())
                }
            }
        }
    }

    private fun fillNv12BufferFromBitmap(buf: ByteBuffer, bitmap: Bitmap, argb: IntArray) {
        val w = bitmap.width
        val h = bitmap.height
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)
        buf.clear()

        val uvStart = w * h
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = argb[y * w + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val yVal = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
                buf.put(y * w + x, yVal.toByte())
                if ((y and 1) == 0 && (x and 1) == 0) {
                    val uVal = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                    val vVal = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                    val uvIdx = uvStart + (y shr 1) * w + x
                    buf.put(uvIdx, uVal.toByte())
                    buf.put(uvIdx + 1, vVal.toByte())
                }
            }
        }
    }

    private fun decodeWavToMonoPcm44k(wavFile: File): ShortArray? {
        return try {
            val bytes = wavFile.readBytes()
            if (bytes.size <= 44) return null
            // Parse WAV header fields for sample rate & channels
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val channels = bb.getShort(22).toInt().coerceAtLeast(1)
            val sampleRate = bb.getInt(24).coerceAtLeast(8000)

            // Locate 'data' chunk robustly
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

            val totalInSamples = dataSize / (2 * channels)
            val monoIn = ShortArray(totalInSamples)
            for (i in 0 until totalInSamples) {
                var sum = 0
                for (ch in 0 until channels) {
                    sum += bb.getShort(dataOffset + (i * channels + ch) * 2).toInt()
                }
                monoIn[i] = (sum / channels).toShort()
            }
            resampleMono(monoIn, sampleRate, BgmSynthesizer.SAMPLE_RATE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode WAV", e)
            null
        }
    }

    private fun decodeUriToMonoPcm44k(uri: Uri, targetSamples: Int, loop: Boolean): ShortArray? {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIdx = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = i
                    format = f
                    break
                }
            }
            if (audioTrackIdx < 0 || format == null) return null
            extractor.selectTrack(audioTrackIdx)

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val pcmOut = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var sawInputEos = false
            var sawOutputEos = false
            val maxBytesToDecode = targetSamples * 4 * channels

            while (!sawOutputEos && pcmOut.size() < maxBytesToDecode) {
                if (!sawInputEos) {
                    val inIdx = decoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = decoder.dequeueOutputBuffer(info, 5_000L)
                if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outFmt = decoder.outputFormat
                    if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                } else if (outIdx >= 0) {
                    val outBuf = decoder.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        val chunk = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(chunk)
                        pcmOut.write(chunk)
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEos = true
                    }
                }
            }

            val rawBytes = pcmOut.toByteArray()
            if (rawBytes.size < 4) return null
            val bb = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
            val totalFrames = rawBytes.size / (2 * channels.coerceAtLeast(1))
            val mono = ShortArray(totalFrames)
            for (i in 0 until totalFrames) {
                var sum = 0
                for (ch in 0 until channels) {
                    sum += bb.getShort((i * channels + ch) * 2).toInt()
                }
                mono[i] = (sum / channels).toShort()
            }
            val resampled = resampleMono(mono, sampleRate, BgmSynthesizer.SAMPLE_RATE)
            if (resampled.isEmpty()) return null

            val finalOut = ShortArray(targetSamples)
            if (loop) {
                for (i in 0 until targetSamples) {
                    finalOut[i] = resampled[i % resampled.size]
                }
            } else {
                val n = minOf(resampled.size, targetSamples)
                System.arraycopy(resampled, 0, finalOut, 0, n)
            }
            finalOut
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode custom audio URI", e)
            null
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun resampleMono(input: ShortArray, srcRate: Int, dstRate: Int): ShortArray {
        if (srcRate == dstRate || input.isEmpty()) return input
        val outLen = ((input.size.toLong() * dstRate) / srcRate).toInt().coerceAtLeast(1)
        val out = ShortArray(outLen)
        val ratio = srcRate.toDouble() / dstRate
        for (i in 0 until outLen) {
            val pos = i * ratio
            val idx0 = pos.toInt().coerceIn(0, input.lastIndex)
            val idx1 = (idx0 + 1).coerceIn(0, input.lastIndex)
            val frac = pos - idx0
            out[i] = (input[idx0] * (1.0 - frac) + input[idx1] * frac).toInt().toShort()
        }
        return out
    }
}
