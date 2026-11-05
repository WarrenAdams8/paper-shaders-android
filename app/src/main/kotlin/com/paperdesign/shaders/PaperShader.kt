package com.paperdesign.shaders

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Animation clock in Paper's units: `timeMs` advances by `dt_ms * speed`. */
@Stable
class ShaderClock(initialMs: Float = 0f) {
    var timeMs by mutableFloatStateOf(initialMs)
    var fps by mutableFloatStateOf(0f)
        internal set
}

/**
 * Renders [state] with its Paper shader.
 *
 * - API 33+: AGSL [android.graphics.RuntimeShader]. Uniforms are pushed only after an edit or
 *   resize; per frame only `u_time` changes. State is read inside the draw lambda, so edits
 *   and time ticks redraw without recomposing.
 * - API 24-32: a static gradient made from the shader's colors (or the source image).
 *
 * @param playing set false to freeze time (for thumbnails or a pause button).
 */
@Composable
fun PaperShader(
    state: ShaderState,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    clock: ShaderClock = remember(state) { ShaderClock(state.float("frame")) },
    input: ShaderInput? = null,
) {
    val speed = state.float("speed")
    val animate = playing && state.spec.animated && speed != 0f
    LaunchedEffect(clock, animate) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        var frames = 0
        var fpsWindowStart = 0L
        // One callback instance for the whole loop: no per-frame lambda allocation.
        val onFrame: (Long) -> Unit = { now ->
            if (last != 0L) {
                val dtMs = (now - last) / 1_000_000f
                clock.timeMs += dtMs * state.float("speed")
            }
            last = now
            if (fpsWindowStart == 0L) fpsWindowStart = now
            frames++
            val window = now - fpsWindowStart
            if (window >= 500_000_000L) {
                clock.fps = frames * 1e9f / window
                frames = 0
                fpsWindowStart = now
            }
        }
        while (true) withFrameNanos(onFrame)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val program = remember(state.spec) {
            runCatching {
                val t0 = System.nanoTime()
                ShaderProgram(state.spec, ShaderAssets.body(state.spec)).also {
                    Log.d("PaperShader", "compiled ${state.spec.id} in ${(System.nanoTime() - t0) / 1_000_000} ms")
                }
            }
                .onFailure { Log.e("PaperShader", "AGSL compile failed for ${state.spec.id}", it) }
                .getOrNull()
        }
        if (program == null) {
            Box(modifier.fallback(state))
            return
        }
        val brush = remember(program) { ShaderBrush(program.shader) }
        val image = rememberShaderImage(state)
        Box(
            modifier.drawBehind {
                program.update(
                    state = state,
                    width = size.width,
                    height = size.height,
                    density = density,
                    timeMs = clock.timeMs,
                    image = image,
                    input = input,
                )
                // Read so touch/tilt changes redraw (even for static shaders).
                input?.version
                drawRect(brush)
            },
        )
    } else {
        Box(modifier.fallback(state))
    }
}

/**
 * The bitmap bound to `u_image`: the picked image or the spec's sample, run through the spec's
 * preprocessing (on a background thread, cached) when it has one.
 */
@Composable
private fun rememberShaderImage(state: ShaderState): Bitmap? {
    val spec = state.spec
    if (!spec.usesImage) return null
    val source = state.image ?: ShaderAssets.sample(spec.sampleImage)
    val step = spec.preprocess ?: return source
    val processed by produceState(ShaderAssets.cachedProcessed(source, step), source, step) {
        if (value == null) value = withContext(Dispatchers.Default) { ShaderAssets.process(source, step) }
    }
    return processed ?: ShaderAssets.transparentPixel
}

/** Pre-AGSL fallback: a radial gradient of the shader's colors, or the source image. */
private fun Modifier.fallback(state: ShaderState): Modifier = drawWithCache {
    @Suppress("UNUSED_VARIABLE") val v = state.version
    val spec = state.spec
    if (spec.usesImage) {
        val img = (state.image ?: ShaderAssets.sample(spec.sampleImage)).asImageBitmap()
        onDrawBehind {
            val s = maxOf(size.width / img.width, size.height / img.height)
            val dw = (img.width * s).toInt()
            val dh = (img.height * s).toInt()
            drawImage(
                img,
                dstOffset = IntOffset(((size.width - dw) / 2).toInt(), ((size.height - dh) / 2).toInt()),
                dstSize = IntSize(dw, dh),
            )
        }
    } else {
        val colors = buildList {
            spec.params.forEach { p ->
                when (p) {
                    is ColorsParam -> state.colors(p.key).forEach { add(it.toColor()) }
                    is ColorParam -> state.color(p.key)?.let { add(it.toColor()) }
                    else -> Unit
                }
            }
        }.ifEmpty { listOf(Color.Black, Color.DarkGray) }
        val brush = if (colors.size == 1) {
            Brush.linearGradient(listOf(colors[0], colors[0]))
        } else {
            Brush.radialGradient(colors, center = Offset(size.width * 0.5f, size.height * 0.45f), radius = maxOf(size.width, size.height) * 0.8f)
        }
        onDrawBehind { drawRect(brush) }
    }
}

fun ArgbColor.toColor(): Color = Color(argb.toInt())
