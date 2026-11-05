package com.paperdesign.shaders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.warpshader.R
import java.util.concurrent.ConcurrentHashMap

/** Shared, lazily created resources: AGSL bodies, the Paper noise texture, a sample image. */
object ShaderAssets {
    private lateinit var appContext: Context
    private val bodies = ConcurrentHashMap<String, String>()

    fun init(context: Context) {
        if (!::appContext.isInitialized) appContext = context.applicationContext
    }

    fun body(spec: ShaderSpec): String = bodies.getOrPut(spec.asset) {
        appContext.assets.open(spec.asset).bufferedReader().use { it.readText() }
    }

    /** Upstream `getShaderNoiseTexture()`: a 128x128 RGB noise PNG. */
    val noiseBitmap: Bitmap by lazy {
        val opts = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(appContext.resources, R.drawable.paper_noise, opts)
    }

    @get:RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val noiseShader: BitmapShader by lazy {
        BitmapShader(noiseBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }
    }

    /** Bundled sample photo-like image for the image filters. Drawn once, procedurally. */
    val sampleImage: Bitmap by lazy { drawSampleImage(1024, 1024) }

    /** Bundled sample logo-like shape (opaque star on transparency) for the shape-mask shaders. */
    val sampleShape: Bitmap by lazy { drawSampleShape(1024) }

    /** Upstream's `transparentPixel`, shown while an image is still being processed. */
    val transparentPixel: Bitmap by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }

    fun sample(kind: SampleImage): Bitmap = when (kind) {
        SampleImage.PHOTO -> sampleImage
        SampleImage.SHAPE -> sampleShape
    }

    private val processed = object : LinkedHashMap<Pair<Bitmap, ImagePreprocess>, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Bitmap, ImagePreprocess>, Bitmap>) = size > 6
    }

    fun cachedProcessed(source: Bitmap, step: ImagePreprocess): Bitmap? = synchronized(processed) { processed[source to step] }

    /** Runs [step] on [source] (slow: call off the main thread) and caches the result. */
    fun process(source: Bitmap, step: ImagePreprocess): Bitmap {
        cachedProcessed(source, step)?.let { return it }
        val out = step.run(source)
        synchronized(processed) { processed[source to step] = out }
        return out
    }

    private fun drawSampleShape(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            pathEffect = android.graphics.CornerPathEffect(size * 0.06f)
        }
        val path = Path()
        val cx = size / 2f
        val cy = size * 0.53f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) size * 0.46f else size * 0.22f
            val a = -Math.PI / 2 + i * Math.PI / 5
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, p)
        return bmp
    }

    private fun drawSampleImage(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h * 0.7f, intArrayOf(0xFF1B2A6B.toInt(), 0xFFE0607E.toInt(), 0xFFFFC27A.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = RadialGradient(w * 0.62f, h * 0.5f, w * 0.2f, intArrayOf(0xFFFFF4C9.toInt(), 0xFFFFB85C.toInt()), null, Shader.TileMode.CLAMP)
        c.drawCircle(w * 0.62f, h * 0.5f, w * 0.17f, p)
        p.shader = null
        fun ridge(base: Float, amp: Float, freq: Float, phase: Float, color: Int) {
            val path = Path()
            path.moveTo(0f, h.toFloat())
            var x = 0f
            while (x <= w) {
                val t = x / w
                val y = h * base - amp * h * (0.6f * kotlin.math.sin(t * freq + phase) + 0.4f * kotlin.math.sin(t * freq * 2.7f + phase * 1.3f))
                path.lineTo(x, y)
                x += 8f
            }
            path.lineTo(w.toFloat(), h.toFloat())
            path.close()
            p.color = color
            c.drawPath(path, p)
        }
        ridge(0.66f, 0.08f, 5f, 0.4f, 0xFF7A3D6E.toInt())
        ridge(0.76f, 0.07f, 7f, 2.1f, 0xFF3E2457.toInt())
        ridge(0.88f, 0.05f, 9f, 4.0f, 0xFF160F2B.toInt())
        p.color = 0xFFFFFFFF.toInt()
        p.textSize = w * 0.14f
        p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        c.drawText("AGSL", w * 0.5f, h * 0.24f, p)
        return bmp
    }
}
