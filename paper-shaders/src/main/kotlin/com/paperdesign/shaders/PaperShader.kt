package com.paperdesign.shaders

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
 * Renders [state] with its Paper shader, filling [modifier]'s bounds.
 * Same as `Box(modifier.paperShader(state, ...))`; see [paperShader].
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
    Box(modifier.paperShader(state, playing, clock, input))
}

/**
 * Draws [state]'s Paper shader behind this element's content, for any of the catalog shaders.
 *
 * - API 33+: AGSL [android.graphics.RuntimeShader]. Uniforms are pushed only after an edit or
 *   resize; per frame only `u_time` changes. State is read inside the draw lambda, so edits
 *   and time ticks redraw without recomposing.
 * - API 24-32: a static gradient made from the shader's colors (or the source image).
 *
 * @param playing set false to freeze time (for thumbnails or a pause button).
 */
@Suppress("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
@Composable
fun Modifier.paperShader(
    state: ShaderState,
    playing: Boolean = true,
    clock: ShaderClock = remember(state) { ShaderClock(state.float("frame")) },
    input: ShaderInput? = null,
): Modifier {
    ShaderAssets.init(LocalContext.current)
    val speed = state.float("speed")
    // The pre-AGSL fallback is static: skip the frame loop below API 33 to save battery.
    val animate = playing && state.spec.animated && speed != 0f &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
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

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return fallback(state)
    val program = remember(state.spec) { compileProgram(state.spec) } ?: return fallback(state)
    val brush = remember(program) { ShaderBrush(program.shader) }
    val image = rememberShaderImage(state)
    return drawBehind {
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
    }
}

/** Compiles [spec]'s AGSL; logs and returns null on failure so callers can fall back. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun compileProgram(spec: ShaderSpec): ShaderProgram? = runCatching {
    val t0 = System.nanoTime()
    ShaderProgram(spec, ShaderAssets.body(spec)).also {
        Log.d("PaperShader", "compiled ${spec.id} in ${(System.nanoTime() - t0) / 1_000_000} ms")
    }
}
    .onFailure { Log.e("PaperShader", "AGSL compile failed for ${spec.id}", it) }
    .getOrNull()

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
        val colors = fallbackColors(state).map { it.toColor() }
        val brush = if (colors.size == 1) {
            Brush.linearGradient(listOf(colors[0], colors[0]))
        } else {
            Brush.radialGradient(colors, center = Offset(size.width * 0.5f, size.height * 0.45f), radius = maxOf(size.width, size.height) * 0.8f)
        }
        onDrawBehind { drawRect(brush) }
    }
}

/**
 * Colors for the pre-AGSL gradient: every color and color-list param of the shader, in spec
 * order, or black to dark gray when the shader has none. Shared by Compose and [PaperShaderView].
 */
fun fallbackColors(state: ShaderState): List<ArgbColor> = buildList {
    state.spec.params.forEach { p ->
        when (p) {
            is ColorsParam -> addAll(state.colors(p.key))
            is ColorParam -> state.color(p.key)?.let { add(it) }
            else -> Unit
        }
    }
}.ifEmpty { listOf(ArgbColor(0xFF000000), ArgbColor(0xFF444444)) }

/** This color as a Compose [Color]. */
fun ArgbColor.toColor(): Color = Color(argb.toInt())

/** A Compose [Color] as an [ArgbColor], for [ShaderState.set] and presets. */
fun Color.toArgbColor(): ArgbColor = ArgbColor(toArgb().toLong() and 0xFFFFFFFFL)

/**
 * A [ShaderState] for [spec] that survives recomposition. A new state is made when [spec] or
 * [preset] changes. (Values are not saved across configuration changes or process death; keep
 * your own copy of [ShaderState.snapshot] if you need that.)
 */
@Composable
fun rememberShaderState(spec: ShaderSpec, preset: Preset = spec.defaultPreset): ShaderState =
    remember(spec, preset) { ShaderState(spec, preset) }
