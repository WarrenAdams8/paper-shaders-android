package com.paperdesign.warp

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Parameters configuring the Paper Design Warp Shader.
 *
 * @property colors List of up to 10 colors for the warped gradient.
 * @property proportion Blend point distribution between colors (0.0 to 1.0, 0.5 is equal distribution).
 * @property softness Sharpness of color transitions (0.0 for hard edge, 1.0 for smooth gradient).
 * @property distortion Strength of the noise-based liquid distortion (0.0 to 1.0).
 * @property swirl Strength of the swirl vortex distortion (0.0 to 1.0).
 * @property swirlIterations Number of layered swirl passes (2 to 20).
 * @property shape Base pattern type (Checks, Stripes, or Edge).
 * @property shapeScale Zoom/frequency of the base pattern (0.0 to 1.0).
 * @property speed Animation playback pace multiplier (1.0 = normal, 0.0 = paused).
 * @property scale Overall zoom level of the graphics (0.01 to 4.0).
 * @property rotation Overall rotation angle of the pattern in degrees (0 to 360).
 * @property offsetX Horizontal offset of the pattern center (-1.0 to 1.0).
 * @property offsetY Vertical offset of the pattern center (-1.0 to 1.0).
 */
@Immutable
data class WarpParams(
    val colors: List<Color> = WarpPresets.Default.colors,
    val proportion: Float = 0.45f,
    val softness: Float = 1.0f,
    val distortion: Float = 0.25f,
    val swirl: Float = 0.8f,
    val swirlIterations: Int = 10,
    val shape: WarpShape = WarpShape.Checks,
    val shapeScale: Float = 0.1f,
    val speed: Float = 1.0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
) {
    init {
        require(colors.isNotEmpty()) { "colors list cannot be empty" }
        require(colors.size <= 10) { "maximum of 10 colors is supported by the shader" }
        require(swirlIterations in 1..20) { "swirlIterations must be between 1 and 20" }
    }

    /**
     * Converts colors into a flat float array suitable for AGSL float4[10] uniform binding.
     * Each color is represented as 4 consecutive floats: [R, G, B, A].
     */
    fun toColorFloatArray(): FloatArray {
        val array = FloatArray(40) // 10 colors * 4 channels
        for (i in 0 until minOf(colors.size, 10)) {
            val c = colors[i]
            val offset = i * 4
            array[offset] = c.red
            array[offset + 1] = c.green
            array[offset + 2] = c.blue
            array[offset + 3] = c.alpha
        }
        return array
    }
}
