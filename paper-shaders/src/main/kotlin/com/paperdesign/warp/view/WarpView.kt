package com.paperdesign.warp.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import android.annotation.SuppressLint
import androidx.core.view.isVisible
import androidx.annotation.RequiresApi
import com.paperdesign.warp.WarpParams
import com.paperdesign.warp.WarpPresets
import com.paperdesign.warp.WarpShader

/**
 * Traditional Android [View] for rendering the Paper Design Warp Shader in XML layouts.
 */
class WarpView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var runtimeShader: RuntimeShader? = null
    private var colorBuffer: FloatArray = FloatArray(40)

    private var lastFrameNanos: Long = 0L
    private var elapsedSeconds: Float = 0f
    private var isAnimating: Boolean = false

    var params: WarpParams = WarpPresets.Default
        set(value) {
            field = value
            colorBuffer = value.toColorFloatArray()
            invalidate()
        }

    var isPlaying: Boolean = true
        set(value) {
            field = value
            if (value) startAnimation() else stopAnimation()
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            initShader()
        }
        colorBuffer = params.toColorFloatArray()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initShader() {
        val shader = WarpShader.createRuntimeShader()
        runtimeShader = shader
        paint.shader = shader
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlaying) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && isPlaying) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    private fun startAnimation() {
        if (!isAnimating && isAttachedToWindow && isVisible) {
            isAnimating = true
            lastFrameNanos = 0L
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun stopAnimation() {
        if (isAnimating) {
            isAnimating = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isAnimating) return

        if (lastFrameNanos != 0L) {
            val dt = (frameTimeNanos - lastFrameNanos) * 1e-9f
            elapsedSeconds += dt * params.speed
            invalidate()
        }
        lastFrameNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    private var fallbackW = -1f
    private var fallbackH = -1f
    private var fallbackParams: WarpParams? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null) {
            WarpShader.updateUniforms(
                shader = runtimeShader!!,
                width = w,
                height = h,
                timeSeconds = elapsedSeconds,
                params = params,
                colorBuffer = colorBuffer
            )
            canvas.drawRect(0f, 0f, w, h, paint)
        } else {
            // Fallback for API < 33: rebuild the gradient only when size or params change.
            if (fallbackW != w || fallbackH != h || fallbackParams != params) {
                val colorsInt = IntArray(params.colors.size) { i ->
                    val c = params.colors[i]
                    android.graphics.Color.argb(
                        (c.alpha * 255).toInt(),
                        (c.red * 255).toInt(),
                        (c.green * 255).toInt(),
                        (c.blue * 255).toInt()
                    )
                }
                val radius = maxOf(w, h) * 0.75f * params.scale
                @SuppressLint("DrawAllocation") // Cached: only rebuilt on size or param changes.
                val gradient = RadialGradient(w * 0.5f, h * 0.5f, radius, colorsInt, null, Shader.TileMode.CLAMP)
                paint.shader = gradient
                fallbackW = w
                fallbackH = h
                fallbackParams = params
            }
            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }
}
