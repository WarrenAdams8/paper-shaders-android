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
