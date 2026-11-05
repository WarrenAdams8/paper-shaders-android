package com.paperdesign.warp

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WarpShaderTest {

    @Test
    fun testWarpPresetsExistAndValid() {
        val presets = WarpPresets.AllPresets
        assertEquals(6, presets.size)

        for ((name, params) in presets) {
            assertTrue("Preset $name colors should not be empty", params.colors.isNotEmpty())
            assertTrue("Preset $name colors <= 10", params.colors.size <= 10)
            assertTrue("Preset $name swirlIterations between 1 and 20", params.swirlIterations in 1..20)
            assertTrue("Preset $name distortion between 0 and 1", params.distortion in 0f..1f)
            assertTrue("Preset $name proportion between 0 and 1", params.proportion in 0f..1f)
        }
    }

    @Test
    fun testWarpShapeMapping() {
        assertEquals(0f, WarpShape.Checks.shaderValue, 0.001f)
        assertEquals(1f, WarpShape.Stripes.shaderValue, 0.001f)
        assertEquals(2f, WarpShape.Edge.shaderValue, 0.001f)

        assertEquals(WarpShape.Checks, WarpShape.fromValue(0.1f))
        assertEquals(WarpShape.Stripes, WarpShape.fromValue(0.9f))
        assertEquals(WarpShape.Edge, WarpShape.fromValue(1.8f))
    }

    @Test
    fun testColorFloatArraySerialization() {
        val colors = listOf(
            Color(1f, 0f, 0f, 1f), // Red
            Color(0f, 1f, 0f, 0.5f), // Green with 0.5 alpha
            Color(0f, 0f, 1f, 1f)  // Blue
        )
        val params = WarpParams(colors = colors)
        val floatArray = params.toColorFloatArray()

        assertEquals(40, floatArray.size)

        // Color 0: Red
        assertEquals(1f, floatArray[0], 0.001f)
        assertEquals(0f, floatArray[1], 0.001f)
        assertEquals(0f, floatArray[2], 0.001f)
        assertEquals(1f, floatArray[3], 0.001f)

        // Color 1: Green with 0.5 alpha
        assertEquals(0f, floatArray[4], 0.001f)
        assertEquals(1f, floatArray[5], 0.001f)
        assertEquals(0f, floatArray[6], 0.001f)
        assertEquals(0.5f, floatArray[7], 0.01f)

        // Color 2: Blue
        assertEquals(0f, floatArray[8], 0.001f)
        assertEquals(0f, floatArray[9], 0.001f)
        assertEquals(1f, floatArray[10], 0.001f)
        assertEquals(1f, floatArray[11], 0.001f)

        // Unused slots are initialized to 0
        assertEquals(0f, floatArray[12], 0.001f)
        assertEquals(0f, floatArray[39], 0.001f)
    }

    @Test
    fun testWarpParamsValidation() {
        try {
            WarpParams(colors = emptyList())
            fail("Expected IllegalArgumentException for empty colors")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("empty") == true)
        }

        try {
            WarpParams(colors = List(11) { Color.Black })
            fail("Expected IllegalArgumentException for >10 colors")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("10 colors") == true)
        }
    }

    @Test
    fun testShaderSourceIntegrity() {
        val src = WarpShader.WARP_AGSL_SRC
        assertNotNull(src)
        assertTrue("Shader source must contain u_resolution", src.contains("u_resolution"))
        assertTrue("Shader source must contain u_time", src.contains("u_time"))
        assertTrue("Shader source must contain u_colors", src.contains("u_colors[10]"))
        assertTrue("Shader source must contain u_colorsCount", src.contains("u_colorsCount"))
        assertTrue("Shader source must contain u_swirlIterations", src.contains("u_swirlIterations"))
        assertTrue("Shader source must contain main entry point", src.contains("half4 main(float2 fragCoord)"))
        assertTrue("Shader source must contain valueNoise", src.contains("valueNoise"))
        assertTrue("Shader source must contain hash21", src.contains("hash21"))
    }
}
