package com.paperdesign.shaders

import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks the CPU port of Paper's vertex-shader sizing against hand-computed values. */
class PaperSizingTest {

    private fun eval(rows: FloatArray, x: Float, y: Float) =
        floatArrayOf(rows[0] * x + rows[1] * y + rows[2], rows[3] * x + rows[4] * y + rows[5])

    private fun sizing(fit: Float, scale: Float = 1f, rotation: Float = 0f, offsetX: Float = 0f) =
        PaperSizing().apply {
            compute(
                width = 1000f, height = 2000f, pixelRatio = 2f, imageAspectRatio = 1f,
                fit = fit, scale = scale, rotation = rotation, offsetX = offsetX, offsetY = 0f,
                originX = 0.5f, originY = 0.5f, worldWidth = 0f, worldHeight = 0f,
            )
        }

    @Test
    fun patternUv_isCenteredAndCountsCssPixelsOverHundred() {
        val s = sizing(fit = 0f)
        val center = eval(s.patternUV, 500f, 1000f)
        assertEquals(0f, center[0], 1e-5f)
        assertEquals(0f, center[1], 1e-5f)
        // 100 device px at pixelRatio 2 = 50 CSS px = 0.5 pattern units; GL y points up.
        val right = eval(s.patternUV, 600f, 1000f)
        assertEquals(0.5f, right[0], 1e-4f)
        val down = eval(s.patternUV, 500f, 1100f)
        assertEquals(-0.5f, down[1], 1e-4f)
    }

    @Test
    fun objectUv_containSpansHalfUnitToTheNarrowEdge() {
        val s = sizing(fit = 1f)
        val left = eval(s.objectUV, 0f, 1000f)
        assertEquals(-0.5f, left[0], 1e-4f)
        assertEquals(1000f, s.objectBoxSize[0], 1e-3f)
    }

    @Test
    fun objectUv_scaleAndOffsetFollowTheVertexShader() {
        // uv / scale, offset applied before scaling: (0 + -offsetX) / 2
        val s = sizing(fit = 1f, scale = 2f, offsetX = 0.2f)
        val center = eval(s.objectUV, 500f, 1000f)
        assertEquals(-0.1f, center[0], 1e-4f)
    }

    @Test
    fun rotation_by90DegreesSwapsAxes() {
        val s = sizing(fit = 1f, rotation = 90f)
        val p = eval(s.objectUV, 1000f, 1000f) // right edge in GL: uv = (0.5, 0)
        assertEquals(0f, p[0], 1e-4f)
        assertEquals(0.5f, p[1], 1e-4f)
    }

    @Test
    fun pixelSize_matchesOneDevicePixelInPatternSpace() {
        val s = sizing(fit = 0f)
        assertEquals(0.005f, s.patternPx, 1e-6f)
    }
}
