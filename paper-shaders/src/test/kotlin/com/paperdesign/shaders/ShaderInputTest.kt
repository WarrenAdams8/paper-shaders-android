package com.paperdesign.shaders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pointer, tap ring buffer and tilt filtering in [ShaderInput]. */
class ShaderInputTest {

    @Test
    fun pointer_tracksPositionAndPressedFlag() {
        val input = ShaderInput()
        input.pointerDown(10f, 20f)
        assertEquals(listOf(10f, 20f, 1f), input.pointer.toList())
        input.pointerMove(30f, 40f)
        assertEquals(listOf(30f, 40f, 1f), input.pointer.toList())
        input.pointerUp()
        assertEquals(listOf(30f, 40f, 0f), input.pointer.toList())
    }

    @Test
    fun everyChange_bumpsVersion() {
        val input = ShaderInput()
        var v = input.version
        input.pointerDown(0f, 0f); assertEquals(++v, input.version)
        input.pointerMove(1f, 1f); assertEquals(++v, input.version)
        input.pointerUp(); assertEquals(++v, input.version)
        input.setTilt(0.1f, 0.2f); assertEquals(++v, input.version)
        input.resetTilt(); assertEquals(++v, input.version)
        input.parallax = 0.5f; assertEquals(++v, input.version)
    }

    @Test
    fun fillTouches_marksUnusedSlotsDead() {
        val input = ShaderInput()
        input.fillTouches(1_000_000_000L)
        for (i in 0 until ShaderInput.MAX_TOUCHES) assertEquals(0f, input.touches[i * 4 + 3])
    }

    @Test
    fun taps_fillSlotsAndWrapAround() {
        val input = ShaderInput()
        repeat(ShaderInput.MAX_TOUCHES + 2) { input.pointerDown(it.toFloat(), 0f) }
        input.fillTouches(0L)
        val xs = (0 until ShaderInput.MAX_TOUCHES).map { input.touches[it * 4] }.toSet()
        // The two oldest taps were overwritten by the newest two.
        assertTrue(0f !in xs && 1f !in xs)
        assertTrue(ShaderInput.MAX_TOUCHES.toFloat() in xs && (ShaderInput.MAX_TOUCHES + 1).toFloat() in xs)
        for (i in 0 until ShaderInput.MAX_TOUCHES) assertEquals(1f, input.touches[i * 4 + 3])
    }

    @Test
    fun tapAge_isMeasuredInSeconds() {
        val input = ShaderInput()
        input.pointerDown(5f, 5f) // SystemClock returns 0 on the JVM
        input.fillTouches(2_500_000_000L)
        assertEquals(2.5f, input.touches[2], 1e-4f)
    }

    @Test
    fun firstTiltSample_setsBaselineSoRelativeTiltStartsAtZero() {
        val input = ShaderInput()
        input.setTilt(0.3f, -0.4f)
        assertEquals(0.3f, input.tilt[0], 1e-6f)
        assertEquals(-0.4f, input.tilt[1], 1e-6f)
        assertEquals(0f, input.tiltRelative[0], 1e-6f)
        assertEquals(0f, input.tiltRelative[1], 1e-6f)
    }

    @Test
    fun tilt_isLowPassFilteredTowardNewSamples() {
        val input = ShaderInput()
        input.setTilt(0f, 0f)
        input.setTilt(1f, 0f)
        assertTrue(input.tilt[0] > 0f && input.tilt[0] < 1f)
        repeat(100) { input.setTilt(1f, 0f) }
        assertEquals(1f, input.tilt[0], 1e-3f)
        assertEquals(1f, input.tiltRelative[0], 1e-3f)
    }

    @Test
    fun resetTilt_clearsBaseline() {
        val input = ShaderInput()
        input.setTilt(0.5f, 0.5f)
        input.resetTilt()
        assertEquals(listOf(0f, 0f), input.tilt.toList())
        input.setTilt(-0.2f, 0.1f)
        assertEquals(0f, input.tiltRelative[0], 1e-6f)
    }
}
