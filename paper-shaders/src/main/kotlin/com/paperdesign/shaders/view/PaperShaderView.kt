package com.paperdesign.shaders.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.compose.runtime.snapshots.ObserverHandle
import com.paperdesign.shaders.R
import androidx.compose.runtime.snapshots.Snapshot
import com.paperdesign.shaders.ShaderAssets
import com.paperdesign.shaders.ShaderProgram
import com.paperdesign.shaders.ShaderSpec
import com.paperdesign.shaders.ShaderState
import com.paperdesign.shaders.compileProgram
import com.paperdesign.shaders.fallbackColors
import com.paperdesign.shaders.specs.ShaderCatalog
import java.util.concurrent.Executors

/**
 * Classic [View] that renders any Paper shader from [ShaderCatalog], for XML layouts.
 *
 * ```xml
 * <com.paperdesign.shaders.view.PaperShaderView
 *     android:layout_width="match_parent"
 *     android:layout_height="240dp"
 *     app:shader="mesh-gradient" />
 * ```
 *
 * Edit [state] (`view.state.set("speed", 0.5f)`, `view.state.applyPreset(...)`) and the view
 * redraws on its own. API 33+ uses AGSL; API 24-32 draws the same static gradient (or source
 * image) as the Compose fallback.
 */
class PaperShaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var program: ShaderProgram? = null
    private var programSpec: ShaderSpec? = null
    private var drawnVersion = -1
    private var observer: ObserverHandle? = null
    private var lastFrameNanos = 0L
    private var animating = false

    /** Animation time in Paper's units (milliseconds of shader time). */
    var timeMs: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    /** Live parameters. Replacing it switches shader; editing it redraws. */
    var state: ShaderState = ShaderState(ShaderCatalog.all.first { it.id == "mesh-gradient" })
        set(value) {
            field = value
            timeMs = value.float("frame")
            drawnVersion = -1
            updateAnimation()
            invalidate()
        }

    var isPlaying: Boolean = true
        set(value) {
            field = value
            updateAnimation()
        }

    /** Switches to the catalog shader with this id (e.g. `"warp"`), at its default preset. */
    fun setShader(id: String) {
        state = ShaderState(requireNotNull(ShaderCatalog.all.firstOrNull { it.id == id }) { "Unknown shader id: $id" })
    }

    // Image shaders: the preprocessed image, produced off the main thread.
    private var image: Bitmap? = null
    private var imageKey: Any? = null

    init {
        ShaderAssets.init(context)
        context.withStyledAttributes(attrs, R.styleable.PaperShaderView, defStyleAttr) {
            getString(R.styleable.PaperShaderView_paperShader)?.let { setShader(it) }
            isPlaying = getBoolean(R.styleable.PaperShaderView_paperPlaying, true)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Redraw when any ShaderState edit is applied (state uses Compose snapshot state).
        observer = Snapshot.registerApplyObserver { _, _ ->
            if (state.version != drawnVersion) post { invalidate() }
        }
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        observer?.dispose()
        observer = null
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimation()
    }

    private fun updateAnimation() {
        val shouldAnimate = isPlaying && isAttachedToWindow && isShown && state.spec.animated
        if (shouldAnimate && !animating) {
            animating = true
            lastFrameNanos = 0L
            Choreographer.getInstance().postFrameCallback(this)
        } else if (!shouldAnimate) {
            stopAnimation()
        }
    }

    private fun stopAnimation() {
        if (animating) {
            animating = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!animating) return
        if (lastFrameNanos != 0L) {
            val speed = state.float("speed")
            if (speed != 0f) timeMs += (frameTimeNanos - lastFrameNanos) / 1_000_000f * speed
        }
        lastFrameNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        drawnVersion = state.version
        val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) programFor(state.spec) else null
        if (p != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            p.update(state, w, h, resources.displayMetrics.density, timeMs, imageFor(state))
            paint.shader = p.shader
        } else {
            paint.shader = fallbackShader(w, h)
        }
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun programFor(spec: ShaderSpec): ShaderProgram? {
        if (programSpec != spec && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            program = compileProgram(spec)
            programSpec = spec
        }
        return program
    }

    private fun sourceImage(s: ShaderState): Bitmap = s.image ?: ShaderAssets.sample(s.spec.sampleImage)

    /** Mirrors the Compose path: source image, preprocessed on a worker and cached. */
    private fun imageFor(s: ShaderState): Bitmap? {
        if (!s.spec.usesImage) return null
        val source = sourceImage(s)
        val step = s.spec.preprocess ?: return source
        val key = source to step
        if (key == imageKey) return image ?: ShaderAssets.transparentPixel
        imageKey = key
        image = ShaderAssets.cachedProcessed(source, step)
        if (image == null) {
            worker.execute {
                val out = ShaderAssets.process(source, step)
                main.post {
                    if (imageKey == key) {
                        image = out
                        invalidate()
                    }
                }
            }
        }
        return image ?: ShaderAssets.transparentPixel
    }

    private fun fallbackShader(w: Float, h: Float): Shader {
        val s = state
        if (s.spec.usesImage) {
            val img = sourceImage(s)
            val scale = maxOf(w / img.width, h / img.height)
            return BitmapShader(img, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(
                    Matrix().apply {
                        setScale(scale, scale)
                        postTranslate((w - img.width * scale) / 2f, (h - img.height * scale) / 2f)
                    },
                )
            }
        }
        val colors = fallbackColors(s).map { it.argb.toInt() }.toIntArray()
        val stops = if (colors.size == 1) intArrayOf(colors[0], colors[0]) else colors
        return RadialGradient(w * 0.5f, h * 0.45f, maxOf(w, h) * 0.8f, stops, null, Shader.TileMode.CLAMP)
    }

    private companion object {
        val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "PaperShaderView-image").apply { isDaemon = true } }
        val main = Handler(Looper.getMainLooper())
    }
}
