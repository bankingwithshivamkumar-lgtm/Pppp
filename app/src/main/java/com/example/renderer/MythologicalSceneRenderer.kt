package com.example.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.CameraShot
import com.example.model.EnvironmentType
import com.example.model.SceneItem
import com.example.model.Speaker
import com.example.model.VisualSettings
import com.example.model.splitIntoCaptionPhrases
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MythologicalSceneRenderer(private val context: Context) {

    private val ramDefaultBitmap: Bitmap by lazy {
        loadDrawableScaled(R.drawable.img_char_ram, 768)
    }
    private val lakshmanDefaultBitmap: Bitmap by lazy {
        loadDrawableScaled(R.drawable.img_char_lakshman, 768)
    }
    private val hanumanDefaultBitmap: Bitmap by lazy {
        loadDrawableScaled(R.drawable.img_char_hanuman, 768)
    }

    private val customBitmapCache = mutableMapOf<String, Bitmap?>()

    private val devanagariTypeface: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.noto_sans_devanagari) ?: Typeface.DEFAULT_BOLD
    }

    private val displayTypeface: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.cinzel) ?: Typeface.SERIF
    }

    private val dimColorFilter: ColorMatrixColorFilter by lazy {
        val cm = ColorMatrix().apply {
            setSaturation(0.75f)
            val scale = 0.72f
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, 0f,
                        0f, scale, 0f, 0f, 0f,
                        0f, 0f, scale, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        ColorMatrixColorFilter(cm)
    }

    private val overShoulderSilhouetteFilter =
        PorterDuffColorFilter(0xCC090D16.toInt(), PorterDuff.Mode.SRC_ATOP)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val speakerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private fun loadDrawableScaled(resId: Int, maxDim: Int): Bitmap {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, opts)
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
            ?: Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    }

    private fun loadUriBitmap(uriString: String?, maxDim: Int = 960): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        if (customBitmapCache.containsKey(uriString)) {
            return customBitmapCache[uriString]
        }
        val loaded = try {
            val uri = Uri.parse(uriString)
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var sample = 1
            while (boundsOpts.outWidth / sample > maxDim || boundsOpts.outHeight / sample > maxDim) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        }
        if (customBitmapCache.size > 12) {
            customBitmapCache.clear()
        }
        customBitmapCache[uriString] = loaded
        return loaded
    }

    private fun characterBitmap(speaker: Speaker, visualSettings: VisualSettings): Bitmap {
        val customUri = when (speaker) {
            Speaker.RAM -> visualSettings.customRamImageUri
            Speaker.LAKSHMAN -> visualSettings.customLakshmanImageUri
            Speaker.HANUMAN -> visualSettings.customHanumanImageUri
        }
        loadUriBitmap(customUri, 768)?.let { return it }
        return when (speaker) {
            Speaker.RAM -> ramDefaultBitmap
            Speaker.LAKSHMAN -> lakshmanDefaultBitmap
            Speaker.HANUMAN -> hanumanDefaultBitmap
        }
    }

    fun drawFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        scene: SceneItem,
        sceneProgress: Float,
        globalTimeSec: Float,
        totalProgress: Float,
        visualSettings: VisualSettings,
        isPlaying: Boolean
    ) {
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Pitch-black 9:16 letterbox canvas
        canvas.drawColor(Color.BLACK)

        // Centered 4:3 / square-ish mythological scene frame
        val sceneSideMargin = w * 0.025f
        val sceneWidth = w - sceneSideMargin * 2f
        val sceneHeight = minOf(sceneWidth * 0.92f, h * 0.50f)
        val sceneTop = (h - sceneHeight) * 0.52f
        val sceneRect = RectF(sceneSideMargin, sceneTop, sceneSideMargin + sceneWidth, sceneTop + sceneHeight)

        // 2. Render scene interior with camera motion
        canvas.save()
        canvas.clipRect(sceneRect)

        val easedProgress = easeInOut(sceneProgress.coerceIn(0f, 1f))
        val (camZoom, camPanX, camPanY) = computeCameraTransform(
            shot = scene.cameraShot,
            speaker = scene.speaker,
            progress = easedProgress,
            sceneWidth = sceneWidth,
            sceneHeight = sceneHeight
        )

        canvas.save()
        canvas.translate(sceneRect.centerX() + camPanX, sceneRect.centerY() + camPanY)
        canvas.scale(camZoom, camZoom)
        canvas.translate(-sceneRect.centerX(), -sceneRect.centerY())

        // Background environment or custom scene image
        val customBg = loadUriBitmap(scene.customSceneImageUri, 1080)
        if (customBg != null) {
            drawBitmapCover(canvas, customBg, sceneRect)
            // Subtle atmospheric dark tint so characters pop
            bgPaint.shader = null
            bgPaint.color = 0x440B0F19
            canvas.drawRect(sceneRect, bgPaint)
        } else {
            drawEnvironmentBackdrop(canvas, sceneRect, scene.environment, globalTimeSec)
        }

        // Draw characters in dramatic composition
        drawSceneCharacters(
            canvas = canvas,
            sceneRect = sceneRect,
            scene = scene,
            globalTimeSec = globalTimeSec,
            visualSettings = visualSettings,
            isPlaying = isPlaying
        )

        // Foreground atmospheric haze & embers/fireflies
        drawForegroundGlowAndParticles(
            canvas = canvas,
            sceneRect = sceneRect,
            environment = scene.environment,
            globalTimeSec = globalTimeSec,
            intensity = visualSettings.particleIntensity
        )

        canvas.restore() // end camera transform

        // Subtle cinematic edge vignette inside scene frame
        drawSceneVignette(canvas, sceneRect)

        // Optional channel watermark inside scene frame
        if (visualSettings.showWatermark && visualSettings.watermarkText.isNotBlank()) {
            watermarkPaint.typeface = displayTypeface
            watermarkPaint.textSize = w * 0.030f
            watermarkPaint.color = Color.argb(
                (visualSettings.watermarkOpacity.coerceIn(0.1f, 0.9f) * 255).toInt(),
                253, 230, 138
            )
            watermarkPaint.setShadowLayer(4f, 0f, 2f, 0xAA000000.toInt())
            canvas.drawText(
                visualSettings.watermarkText,
                sceneRect.centerX(),
                sceneRect.bottom - sceneHeight * 0.055f,
                watermarkPaint
            )
            watermarkPaint.clearShadowLayer()
        }

        canvas.restore() // end clipRect

        // 3. Ornamental golden border around the centered scene frame
        borderPaint.color = 0xFFD4AF37.toInt()
        borderPaint.strokeWidth = maxOf(2f, w * 0.0045f)
        canvas.drawRect(sceneRect, borderPaint)

        val innerPad = maxOf(4f, w * 0.009f)
        val innerRect = RectF(
            sceneRect.left + innerPad,
            sceneRect.top + innerPad,
            sceneRect.right - innerPad,
            sceneRect.bottom - innerPad
        )
        borderPaint.color = 0x55F59E0B
        borderPaint.strokeWidth = maxOf(1f, w * 0.002f)
        canvas.drawRect(innerRect, borderPaint)

        // 4. Top Black Bar: Synced Phrase-by-Phrase Hindi Caption
        drawTopCaptionBar(
            canvas = canvas,
            width = w,
            topBarBottom = sceneRect.top,
            scene = scene,
            sceneProgress = sceneProgress,
            visualSettings = visualSettings
        )

        // 5. Bottom Black Bar: Speaker Nameplate + Shot Indicator + Progress Hairline
        drawBottomBar(
            canvas = canvas,
            width = w,
            height = h,
            bottomBarTop = sceneRect.bottom,
            scene = scene,
            totalProgress = totalProgress
        )
    }

    private fun computeCameraTransform(
        shot: CameraShot,
        speaker: Speaker,
        progress: Float,
        sceneWidth: Float,
        sceneHeight: Float
    ): Triple<Float, Float, Float> {
        return when (shot) {
            CameraShot.CLOSE_UP -> {
                val zoom = 1.15f + progress * 0.13f
                val panY = sceneHeight * 0.03f * progress
                Triple(zoom, 0f, panY)
            }
            CameraShot.TWO_SHOT -> {
                val zoom = 1.03f + progress * 0.08f
                val dir = if (speaker == Speaker.RAM) -1f else 1f
                val panX = dir * sceneWidth * 0.025f * progress
                Triple(zoom, panX, 0f)
            }
            CameraShot.WIDE_CAMP -> {
                val zoom = 1.0f + progress * 0.06f
                Triple(zoom, 0f, -sceneHeight * 0.015f * progress)
            }
            CameraShot.OVER_SHOULDER -> {
                val zoom = 1.10f + progress * 0.10f
                val panX = sceneWidth * 0.03f * (0.5f - progress)
                Triple(zoom, panX, sceneHeight * 0.01f)
            }
        }
    }

    private fun drawEnvironmentBackdrop(
        canvas: Canvas,
        rect: RectF,
        env: EnvironmentType,
        timeSec: Float
    ) {
        val l = rect.left
        val t = rect.top
        val r = rect.right
        val b = rect.bottom
        val w = rect.width()
        val h = rect.height()

        when (env) {
            EnvironmentType.DUSK_CAMP -> {
                // Warm twilight saffron-crimson to deep indigo sky
                bgPaint.shader = LinearGradient(
                    l, t, l, b,
                    intArrayOf(0xFF1A142E.toInt(), 0xFF5C2327.toInt(), 0xFFB45309.toInt(), 0xFF1F1209.toInt()),
                    floatArrayOf(0f, 0.42f, 0.78f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(rect, bgPaint)

                // Setting sun glow
                val sunCx = l + w * 0.5f
                val sunCy = t + h * 0.52f
                bgPaint.shader = RadialGradient(
                    sunCx, sunCy, w * 0.48f,
                    intArrayOf(0xCCFDE68A.toInt(), 0x66F59E0B, 0x00F59E0B),
                    floatArrayOf(0f, 0.35f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(sunCx, sunCy, w * 0.48f, bgPaint)
                bgPaint.shader = null

                // Distant mountain ridge
                val ridge = Path().apply {
                    moveTo(l, b)
                    lineTo(l, t + h * 0.64f)
                    lineTo(l + w * 0.22f, t + h * 0.52f)
                    lineTo(l + w * 0.45f, t + h * 0.60f)
                    lineTo(l + w * 0.70f, t + h * 0.48f)
                    lineTo(r, t + h * 0.62f)
                    lineTo(r, b)
                    close()
                }
                shapePaint.color = 0xFF28161C.toInt()
                canvas.drawPath(ridge, shapePaint)

                // Royal camp tents silhouettes on sides
                val leftTent = Path().apply {
                    moveTo(l - w * 0.04f, b)
                    lineTo(l + w * 0.08f, t + h * 0.46f)
                    lineTo(l + w * 0.24f, b)
                    close()
                }
                val rightTent = Path().apply {
                    moveTo(r - w * 0.24f, b)
                    lineTo(r - w * 0.08f, t + h * 0.46f)
                    lineTo(r + w * 0.04f, b)
                    close()
                }
                shapePaint.color = 0xFF190F12.toInt()
                canvas.drawPath(leftTent, shapePaint)
                canvas.drawPath(rightTent, shapePaint)

                // Fluttering saffron flags
                val wave = sin(timeSec * 3.5f) * w * 0.015f
                shapePaint.color = 0xFFF59E0B.toInt()
                val leftFlag = Path().apply {
                    moveTo(l + w * 0.08f, t + h * 0.38f)
                    lineTo(l + w * 0.15f, t + h * 0.41f + wave)
                    lineTo(l + w * 0.08f, t + h * 0.45f)
                    close()
                }
                canvas.drawPath(leftFlag, shapePaint)

                // Warm campfire glow at bottom center
                val firePulse = 0.85f + 0.15f * sin(timeSec * 7f)
                bgPaint.shader = RadialGradient(
                    l + w * 0.5f, b, w * 0.45f * firePulse,
                    intArrayOf(0xAAFF7B00.toInt(), 0x44F59E0B, 0x00000000),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(l + w * 0.5f, b, w * 0.45f * firePulse, bgPaint)
                bgPaint.shader = null
            }

            EnvironmentType.NIGHT_FOREST -> {
                // Deep mystical night forest
                bgPaint.shader = LinearGradient(
                    l, t, l, b,
                    intArrayOf(0xFF040814.toInt(), 0xFF0B192C.toInt(), 0xFF102A38.toInt(), 0xFF071214.toInt()),
                    floatArrayOf(0f, 0.45f, 0.80f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(rect, bgPaint)

                // Twinkling stars
                shapePaint.color = 0xFFFFFFFF.toInt()
                for (i in 0 until 28) {
                    val sx = l + ((i * 73 + 19) % 100) / 100f * w
                    val sy = t + ((i * 41 + 7) % 48) / 100f * h
                    val alpha = (110 + (120 * sin(timeSec * 2.1f + i)).toInt()).coerceIn(40, 240)
                    shapePaint.alpha = alpha
                    canvas.drawCircle(sx, sy, w * (0.0025f + (i % 3) * 0.0012f), shapePaint)
                }
                shapePaint.alpha = 255

                // Glowing full moon
                val moonCx = l + w * 0.76f
                val moonCy = t + h * 0.24f
                bgPaint.shader = RadialGradient(
                    moonCx, moonCy, w * 0.28f,
                    intArrayOf(0x66E0F2FE, 0x2293C5FD, 0x0093C5FD),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(moonCx, moonCy, w * 0.28f, bgPaint)
                bgPaint.shader = null
                shapePaint.color = 0xFFF0F9FF.toInt()
                canvas.drawCircle(moonCx, moonCy, w * 0.065f, shapePaint)

                // Panchavati forest trees silhouettes
                shapePaint.color = 0xFF07131A.toInt()
                val treePositions = floatArrayOf(0.06f, 0.18f, 0.82f, 0.94f)
                for (tx in treePositions) {
                    val cx = l + w * tx
                    canvas.drawRect(cx - w * 0.022f, t + h * 0.28f, cx + w * 0.022f, b, shapePaint)
                    canvas.drawCircle(cx, t + h * 0.24f, w * 0.14f, shapePaint)
                    canvas.drawCircle(cx - w * 0.07f, t + h * 0.30f, w * 0.10f, shapePaint)
                    canvas.drawCircle(cx + w * 0.07f, t + h * 0.30f, w * 0.10f, shapePaint)
                }
            }

            EnvironmentType.GOLDEN_ASHRAM -> {
                // Radiant golden ashram dawn
                bgPaint.shader = LinearGradient(
                    l, t, l, b,
                    intArrayOf(0xFF2B1808.toInt(), 0xFF78350F.toInt(), 0xFFD97706.toInt(), 0xFF2E1706.toInt()),
                    floatArrayOf(0f, 0.38f, 0.76f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(rect, bgPaint)

                // Divine sunrays / aura from center top
                val cx = l + w * 0.5f
                val cy = t + h * 0.25f
                bgPaint.shader = RadialGradient(
                    cx, cy, w * 0.65f,
                    intArrayOf(0xCCFEF3C7.toInt(), 0x55F59E0B, 0x00F59E0B),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, cy, w * 0.65f, bgPaint)
                bgPaint.shader = null

                // Sacred carved temple pillars
                shapePaint.color = 0xFF221107.toInt()
                val pillarW = w * 0.07f
                canvas.drawRect(l + w * 0.05f, t, l + w * 0.05f + pillarW, b, shapePaint)
                canvas.drawRect(r - w * 0.05f - pillarW, t, r - w * 0.05f, b, shapePaint)
                // Ornate top arch
                val arch = Path().apply {
                    moveTo(l, t)
                    lineTo(r, t)
                    lineTo(r, t + h * 0.14f)
                    quadTo(l + w * 0.5f, t + h * 0.02f, l, t + h * 0.14f)
                    close()
                }
                canvas.drawPath(arch, shapePaint)

                // Flickering golden diyas on left & right pedestals
                val flicker = 0.88f + 0.12f * sin(timeSec * 9f)
                for (dx in floatArrayOf(0.14f, 0.86f)) {
                    val dcx = l + w * dx
                    val dcy = t + h * 0.78f
                    bgPaint.shader = RadialGradient(
                        dcx, dcy, w * 0.14f * flicker,
                        intArrayOf(0xCCFDE68A.toInt(), 0x44F59E0B, 0x00F59E0B),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(dcx, dcy, w * 0.14f * flicker, bgPaint)
                    bgPaint.shader = null
                    shapePaint.color = 0xFFFDE68A.toInt()
                    canvas.drawOval(
                        dcx - w * 0.015f,
                        dcy - w * 0.032f * flicker,
                        dcx + w * 0.015f,
                        dcy + w * 0.01f,
                        shapePaint
                    )
                }
            }
        }
    }

    private fun drawSceneCharacters(
        canvas: Canvas,
        sceneRect: RectF,
        scene: SceneItem,
        globalTimeSec: Float,
        visualSettings: VisualSettings,
        isPlaying: Boolean
    ) {
        val active = scene.speaker
        val partners = Speaker.entries.filter { it != active }
        val w = sceneRect.width()
        val h = sceneRect.height()

        when (scene.cameraShot) {
            CameraShot.CLOSE_UP -> {
                drawCharacterSprite(
                    canvas = canvas,
                    sceneRect = sceneRect,
                    speaker = active,
                    centerX = sceneRect.centerX(),
                    bottomY = sceneRect.bottom + h * 0.06f,
                    targetHeight = h * 0.94f,
                    isSpeaking = true,
                    isForegroundSilhouette = false,
                    globalTimeSec = globalTimeSec,
                    visualSettings = visualSettings,
                    isPlaying = isPlaying
                )
            }

            CameraShot.TWO_SHOT -> {
                val listener = partners.first()
                // Listener on right, slightly behind
                drawCharacterSprite(
                    canvas = canvas,
                    sceneRect = sceneRect,
                    speaker = listener,
                    centerX = sceneRect.left + w * 0.72f,
                    bottomY = sceneRect.bottom + h * 0.03f,
                    targetHeight = h * 0.76f,
                    isSpeaking = false,
                    isForegroundSilhouette = false,
                    globalTimeSec = globalTimeSec,
                    visualSettings = visualSettings,
                    isPlaying = isPlaying
                )
                // Active speaker on left foreground
                drawCharacterSprite(
                    canvas = canvas,
                    sceneRect = sceneRect,
                    speaker = active,
                    centerX = sceneRect.left + w * 0.31f,
                    bottomY = sceneRect.bottom + h * 0.04f,
                    targetHeight = h * 0.84f,
                    isSpeaking = true,
                    isForegroundSilhouette = false,
                    globalTimeSec = globalTimeSec,
                    visualSettings = visualSettings,
                    isPlaying = isPlaying
                )
            }

            CameraShot.WIDE_CAMP -> {
                // Order: Lakshman left, Ram center, Hanuman right (with active speaker slightly larger/brighter)
                val order = listOf(Speaker.LAKSHMAN, Speaker.RAM, Speaker.HANUMAN)
                val xFracs = floatArrayOf(0.22f, 0.50f, 0.78f)

                // Draw listeners first, then active speaker on top
                order.forEachIndexed { idx, sp ->
                    if (sp != active) {
                        drawCharacterSprite(
                            canvas = canvas,
                            sceneRect = sceneRect,
                            speaker = sp,
                            centerX = sceneRect.left + w * xFracs[idx],
                            bottomY = sceneRect.bottom + h * 0.02f,
                            targetHeight = h * 0.70f,
                            isSpeaking = false,
                            isForegroundSilhouette = false,
                            globalTimeSec = globalTimeSec,
                            visualSettings = visualSettings,
                            isPlaying = isPlaying
                        )
                    }
                }
                val activeIdx = order.indexOf(active).coerceAtLeast(1)
                drawCharacterSprite(
                    canvas = canvas,
                    sceneRect = sceneRect,
                    speaker = active,
                    centerX = sceneRect.left + w * xFracs[activeIdx],
                    bottomY = sceneRect.bottom + h * 0.03f,
                    targetHeight = h * 0.78f,
                    isSpeaking = true,
                    isForegroundSilhouette = false,
                    globalTimeSec = globalTimeSec,
                    visualSettings = visualSettings,
                    isPlaying = isPlaying
                )
            }

            CameraShot.OVER_SHOULDER -> {
                val listener = partners.last()
                // Active speaker illuminated right-center
                drawCharacterSprite(
                    canvas = canvas,
                    sceneRect = sceneRect,
                    speaker = active,
                    centerX = sceneRect.left + w * 0.58f,
                    bottomY = sceneRect.bottom + h * 0.04f,
                    targetHeight = h * 0.86f,
                    isSpeaking = true,
                    isForegroundSilhouette = false,
                    globalTimeSec = globalTimeSec,
                    visualSettings = visualSettings,
                    isPlaying = isPlaying
                )
                // Listener darkened foreground shoulder on bottom-left
                drawCharacterSprite(
                    canvas = canvas,
                    sceneRect = sceneRect,
                    speaker = listener,
                    centerX = sceneRect.left + w * 0.16f,
                    bottomY = sceneRect.bottom + h * 0.20f,
                    targetHeight = h * 0.96f,
                    isSpeaking = false,
                    isForegroundSilhouette = true,
                    globalTimeSec = globalTimeSec,
                    visualSettings = visualSettings,
                    isPlaying = isPlaying
                )
            }
        }
    }

    private fun drawCharacterSprite(
        canvas: Canvas,
        sceneRect: RectF,
        speaker: Speaker,
        centerX: Float,
        bottomY: Float,
        targetHeight: Float,
        isSpeaking: Boolean,
        isForegroundSilhouette: Boolean,
        globalTimeSec: Float,
        visualSettings: VisualSettings,
        isPlaying: Boolean
    ) {
        val bmp = characterBitmap(speaker, visualSettings)
        val aspect = bmp.width.toFloat() / maxOf(1, bmp.height)

        // Gentle breathing + speaking cadence micro-animation
        val speechPulse = if (isSpeaking && isPlaying) {
            1f + 0.014f * sin(globalTimeSec * 8.5f)
        } else {
            1f + 0.005f * sin(globalTimeSec * 2.2f + speaker.ordinal)
        }
        val bobY = if (isSpeaking) {
            -sceneRect.height() * 0.008f * (0.5f + 0.5f * sin(globalTimeSec * 6.5f))
        } else {
            sceneRect.height() * 0.003f * sin(globalTimeSec * 2.0f + speaker.ordinal)
        }

        val drawH = targetHeight * speechPulse
        val drawW = drawH * aspect
        val left = centerX - drawW / 2f
        val top = bottomY - drawH + bobY
        val dst = RectF(left, top, left + drawW, top + drawH)

        // Active speaker golden aura / rim light behind character
        if (isSpeaking) {
            val auraPulse = 0.88f + 0.12f * sin(globalTimeSec * 5.2f)
            val auraRadius = drawW * 0.72f * auraPulse
            val auraCy = dst.top + drawH * 0.36f
            bgPaint.shader = RadialGradient(
                centerX, auraCy, auraRadius,
                intArrayOf(
                    Color.argb(145, Color.red(speaker.auraColorArgb), Color.green(speaker.auraColorArgb), Color.blue(speaker.auraColorArgb)),
                    Color.argb(55, 245, 158, 11),
                    0x00000000
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(centerX, auraCy, auraRadius, bgPaint)
            bgPaint.shader = null
        }

        bitmapPaint.colorFilter = when {
            isForegroundSilhouette -> overShoulderSilhouetteFilter
            !isSpeaking -> dimColorFilter
            else -> null
        }
        bitmapPaint.alpha = if (isForegroundSilhouette) 235 else 255

        val src = Rect(0, 0, bmp.width, bmp.height)
        canvas.drawBitmap(bmp, src, dst, bitmapPaint)
        bitmapPaint.colorFilter = null
        bitmapPaint.alpha = 255
    }

    private fun drawForegroundGlowAndParticles(
        canvas: Canvas,
        sceneRect: RectF,
        environment: EnvironmentType,
        globalTimeSec: Float,
        intensity: Float
    ) {
        val l = sceneRect.left
        val r = sceneRect.right
        val b = sceneRect.bottom
        val w = sceneRect.width()
        val h = sceneRect.height()

        // Soft bottom atmospheric haze so waist-up portraits blend into scene
        val hazeColor = when (environment) {
            EnvironmentType.DUSK_CAMP -> 0xE61F1008.toInt()
            EnvironmentType.NIGHT_FOREST -> 0xE6051015.toInt()
            EnvironmentType.GOLDEN_ASHRAM -> 0xE6281506.toInt()
        }
        bgPaint.shader = LinearGradient(
            l, b - h * 0.24f, l, b,
            intArrayOf(0x00000000, hazeColor),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(l, b - h * 0.24f, r, b, bgPaint)
        bgPaint.shader = null

        // Deterministic floating golden embers / fireflies
        val count = (26 * intensity.coerceIn(0f, 1.5f)).toInt()
        for (i in 0 until count) {
            val speed = 0.08f + (i % 5) * 0.028f
            val baseX = ((i * 53 + 17) % 100) / 100f
            val phase = ((i * 37) % 100) / 100f
            val yFrac = 1f - ((globalTimeSec * speed + phase) % 1f)
            val driftX = 0.025f * sin(globalTimeSec * 1.8f + i * 1.3f)
            val px = l + (baseX + driftX) * w
            val py = sceneRect.top + yFrac * h

            val fade = sin(yFrac * PI.toFloat()).coerceIn(0f, 1f)
            val particleColor = if (environment == EnvironmentType.NIGHT_FOREST) {
                Color.argb((fade * 210).toInt(), 167, 243, 208)
            } else {
                Color.argb((fade * 230).toInt(), 253, 186, 116)
            }
            val radius = w * (0.004f + (i % 3) * 0.0022f)
            shapePaint.color = particleColor
            canvas.drawCircle(px, py, radius, shapePaint)
        }
    }

    private fun drawSceneVignette(canvas: Canvas, rect: RectF) {
        val radius = sqrt(rect.width() * rect.width() + rect.height() * rect.height()) * 0.62f
        bgPaint.shader = RadialGradient(
            rect.centerX(), rect.centerY(), radius,
            intArrayOf(0x00000000, 0x00000000, 0x88000000.toInt()),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(rect, bgPaint)
        bgPaint.shader = null
    }

    private fun drawTopCaptionBar(
        canvas: Canvas,
        width: Float,
        topBarBottom: Float,
        scene: SceneItem,
        sceneProgress: Float,
        visualSettings: VisualSettings
    ) {
        val phrases = splitIntoCaptionPhrases(scene.dialogue)
        val phraseIdx = (sceneProgress * phrases.size).toInt().coerceIn(0, phrases.lastIndex)
        val activePhrase = phrases[phraseIdx]
        val framedCaption = if (activePhrase.isBlank()) {
            ""
        } else {
            "${visualSettings.captionFrameStyle.prefix}$activePhrase${visualSettings.captionFrameStyle.suffix}"
        }
        if (framedCaption.isEmpty()) return

        // Scale font proportionally to canvas width (reference 360dp logical width)
        val scaleFactor = width / 360f
        val textSizePx = visualSettings.captionFontSizeSp.coerceIn(13f, 28f) * scaleFactor

        captionPaint.typeface = devanagariTypeface
        captionPaint.textSize = textSizePx
        captionPaint.color = visualSettings.captionColorArgb
        captionPaint.setShadowLayer(4f * scaleFactor, 0f, 2f * scaleFactor, 0xCC000000.toInt())

        val maxTextWidth = (width * 0.88f).toInt().coerceAtLeast(100)
        val layout = StaticLayout.Builder.obtain(
            framedCaption,
            0,
            framedCaption.length,
            captionPaint,
            maxTextWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f * scaleFactor, 1.15f)
            .setIncludePad(false)
            .build()

        // Slight smooth micro entrance per phrase
        val localPhraseProgress = (sceneProgress * phrases.size) - phraseIdx
        val alpha = (localPhraseProgress * 5f).coerceIn(0.35f, 1f)
        captionPaint.alpha = (alpha * 255).toInt()

        val textX = (width - maxTextWidth) / 2f
        val textY = (topBarBottom - layout.height) * 0.56f

        canvas.save()
        canvas.translate(textX, maxOf(width * 0.04f, textY))
        layout.draw(canvas)
        canvas.restore()
        captionPaint.clearShadowLayer()
        captionPaint.alpha = 255
    }

    private fun drawBottomBar(
        canvas: Canvas,
        width: Float,
        height: Float,
        bottomBarTop: Float,
        scene: SceneItem,
        totalProgress: Float
    ) {
        val barHeight = height - bottomBarTop
        val scaleFactor = width / 360f

        // Speaker Hindi Plaque
        speakerPaint.typeface = devanagariTypeface
        speakerPaint.textSize = 15f * scaleFactor
        speakerPaint.color = scene.speaker.auraColorArgb
        canvas.drawText(
            "— ${scene.speaker.hindiName} —",
            width * 0.5f,
            bottomBarTop + barHeight * 0.34f,
            speakerPaint
        )

        // Subtle environment & camera shot subtitle
        speakerPaint.typeface = displayTypeface
        speakerPaint.textSize = 9.5f * scaleFactor
        speakerPaint.color = 0xFF94A3B8.toInt()
        canvas.drawText(
            "${scene.environment.hindiLabel}  •  ${scene.cameraShot.englishLabel.uppercase()}",
            width * 0.5f,
            bottomBarTop + barHeight * 0.54f,
            speakerPaint
        )

        // Subtle golden progress hairline at bottom
        val barLeft = width * 0.14f
        val barRight = width * 0.86f
        val barY = bottomBarTop + barHeight * 0.74f
        shapePaint.color = 0x33D4AF37
        canvas.drawRoundRect(
            barLeft,
            barY,
            barRight,
            barY + 2.5f * scaleFactor,
            4f,
            4f,
            shapePaint
        )
        shapePaint.color = 0xFFF59E0B.toInt()
        val fillRight = barLeft + (barRight - barLeft) * totalProgress.coerceIn(0f, 1f)
        canvas.drawRoundRect(
            barLeft,
            barY,
            fillRight,
            barY + 2.5f * scaleFactor,
            4f,
            4f,
            shapePaint
        )
    }

    private fun drawBitmapCover(canvas: Canvas, bmp: Bitmap, dstRect: RectF) {
        val viewAspect = dstRect.width() / dstRect.height()
        val bmpAspect = bmp.width.toFloat() / maxOf(1, bmp.height)
        val srcRect = if (bmpAspect > viewAspect) {
            val cropW = (bmp.height * viewAspect).toInt()
            val left = (bmp.width - cropW) / 2
            Rect(left, 0, left + cropW, bmp.height)
        } else {
            val cropH = (bmp.width / viewAspect).toInt()
            val top = (bmp.height - cropH) / 2
            Rect(0, top, bmp.width, top + cropH)
        }
        canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
    }

    private fun easeInOut(t: Float): Float {
        return (0.5f - 0.5f * cos(t * PI)).toFloat()
    }
}
