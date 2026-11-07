package com.paperdesign.shaders

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Live touch and motion input for interactive shaders.
 *
 * Plain arrays mutated in place (no per-event allocations). [version] is bumped on every change
 * and read during draw, so input redraws the shader without recomposing anything.
 *
 * Shader-side uniforms (declared on demand by [ShaderPrelude]):
 * - `u_pointer` float3: last pointer position in fragCoord pixels, and 1 while pressed.
 * - `u_touches[8]` float4: recent taps as (x px, y px, age in seconds, 1 if alive).
 * - `u_tilt` float2: screen-space "down" vector from gravity, each in -1..1 (0,0 when flat or off).
 */
@Stable
class ShaderInput {
    var version by mutableIntStateOf(0)
        private set

    val pointer = FloatArray(3)
    val tilt = FloatArray(2)

    /** Tilt relative to how the phone was held when tilt was switched on (for parallax). */
    val tiltRelative = FloatArray(2)
    private var hasBaseline = false
    private val baseline = FloatArray(2)

    /** Tilt parallax strength for any shader (0 = off): offsets the pattern by tilt * this. */
    var parallax = 0f
        set(value) { field = value; version++ }

    private val tapX = FloatArray(MAX_TOUCHES)
    private val tapY = FloatArray(MAX_TOUCHES)
    private val tapStartNanos = LongArray(MAX_TOUCHES) { Long.MIN_VALUE }
    private var tapNext = 0

    /** Uniform-ready `u_touches` (refreshed by [fillTouches]). */
    val touches = FloatArray(MAX_TOUCHES * 4)

    fun pointerDown(x: Float, y: Float) {
        pointer[0] = x; pointer[1] = y; pointer[2] = 1f
        tapX[tapNext] = x
        tapY[tapNext] = y
        tapStartNanos[tapNext] = SystemClock.elapsedRealtimeNanos()
        tapNext = (tapNext + 1) % MAX_TOUCHES
        version++
    }

    fun pointerMove(x: Float, y: Float) {
        pointer[0] = x; pointer[1] = y
        version++
    }

    fun pointerUp() {
        pointer[2] = 0f
        version++
    }

    /** Low-pass filtered gravity in screen space. */
    fun setTilt(x: Float, y: Float) {
        if (!hasBaseline) {
            tilt[0] = x; tilt[1] = y
            baseline[0] = x; baseline[1] = y
            hasBaseline = true
        }
        tilt[0] += (x - tilt[0]) * TILT_SMOOTHING
        tilt[1] += (y - tilt[1]) * TILT_SMOOTHING
        tiltRelative[0] = tilt[0] - baseline[0]
        tiltRelative[1] = tilt[1] - baseline[1]
        version++
    }

    fun resetTilt() {
        tilt[0] = 0f; tilt[1] = 0f
        tiltRelative[0] = 0f; tiltRelative[1] = 0f
        hasBaseline = false
        version++
    }

    /** Writes tap positions and ages into [touches]. Allocation free; call once per frame. */
    fun fillTouches(nowNanos: Long) {
        for (i in 0 until MAX_TOUCHES) {
            val o = i * 4
            val start = tapStartNanos[i]
            if (start == Long.MIN_VALUE) {
                touches[o + 3] = 0f
                continue
            }
            touches[o] = tapX[i]
            touches[o + 1] = tapY[i]
            touches[o + 2] = (nowNanos - start) / 1e9f
            touches[o + 3] = 1f
        }
    }

    companion object {
        const val MAX_TOUCHES = 8
        private const val TILT_SMOOTHING = 0.2f
    }
}

/**
 * Feeds the gravity sensor into [input] while [enabled]. Uses TYPE_GRAVITY when available,
 * otherwise the accelerometer. Assumes a portrait activity.
 */
@Composable
fun TiltSensorEffect(input: ShaderInput, enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(input, enabled) {
        if (!enabled) {
            input.resetTilt()
            return@DisposableEffect onDispose { }
        }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // The sensor reports the "up" reaction vector; screen-space down is (-x, +y).
                val g = SensorManager.GRAVITY_EARTH
                input.setTilt((-event.values[0] / g).coerceIn(-1f, 1f), (event.values[1] / g).coerceIn(-1f, 1f))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sm.unregisterListener(listener)
            input.resetTilt()
        }
    }
}
