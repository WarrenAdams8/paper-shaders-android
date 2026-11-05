package com.paperdesign.warp

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Jetpack Compose composable rendering the Paper Design Warp Shader.
 *
 * @param modifier The layout modifier for sizing and placement.
 * @param params Full configuration parameter set for the shader (colors, distortion, swirl, etc.).
 * @param isPlaying Whether the animation is currently active.
 * @param timeOverride Optional static time in seconds to freeze or manually control the shader frame.
 */
@Composable
fun Warp(
    modifier: Modifier = Modifier,
    params: WarpParams = WarpPresets.Default,
    isPlaying: Boolean = true,
    timeOverride: Float? = null,
) {
    Box(
        modifier = modifier.warp(
            params = params,
            isPlaying = isPlaying,
            timeOverride = timeOverride
        )
    )
}

/**
 * Modifier extension applying the Warp Shader as a background to any composable.
 *
 * Uses hardware AGSL RuntimeShader on Android 13+ (API 33+) with zero per-frame allocation,
 * and a smooth gradient fallback on earlier Android versions.
 */
@Composable
fun Modifier.warp(
    params: WarpParams = WarpPresets.Default,
    isPlaying: Boolean = true,
    timeOverride: Float? = null,
): Modifier {
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }

    // High performance frame loop that tracks real delta time
    LaunchedEffect(isPlaying, params.speed, timeOverride) {
        if (timeOverride != null || !isPlaying || params.speed == 0f) return@LaunchedEffect

        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) * 1e-9f
                    elapsedSeconds += dt * params.speed
                }
                lastNanos = frameNanos
            }
        }
    }

    val currentTime = timeOverride ?: elapsedSeconds

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val runtimeShader = remember { WarpShader.createRuntimeShader() }
        val shaderBrush = remember(runtimeShader) { ShaderBrush(runtimeShader) }
        val colorBuffer = remember(params.colors) { params.toColorFloatArray() }

        this.drawWithCache {
            WarpShader.updateUniforms(
                shader = runtimeShader,
                width = size.width,
                height = size.height,
                timeSeconds = currentTime,
                params = params,
                colorBuffer = colorBuffer
            )
            onDrawBehind {
                drawRect(shaderBrush)
            }
        }
    } else {
        // Fallback for API < 33 (e.g. Android 12 and below)
        this.drawBehind {
            drawFallbackGradient(params, currentTime)
        }
    }
}

/**
 * Graceful fallback gradient for devices running API < 33 where AGSL is unavailable.
 */
private fun DrawScope.drawFallbackGradient(params: WarpParams, time: Float) {
    val angle = (time * 0.5f) % (2f * Math.PI.toFloat())
    val cx = size.width * (0.5f + 0.1f * cos(angle))
    val cy = size.height * (0.5f + 0.1f * sin(angle))
    val radius = maxOf(size.width, size.height) * 0.75f * params.scale

    val brush = Brush.radialGradient(
        colors = params.colors,
        center = Offset(cx, cy),
        radius = radius
    )
    drawRect(brush)
}
