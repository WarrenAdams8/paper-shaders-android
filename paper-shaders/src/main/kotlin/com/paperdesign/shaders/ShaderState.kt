package com.paperdesign.shaders

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Live, editable parameter values for one shader.
 *
 * Every edit bumps [version]. The renderer compares it with the version it last uploaded,
 * so uniforms are only re-sent after a change, never on every frame.
 */
@Stable
class ShaderState(val spec: ShaderSpec, preset: Preset = spec.defaultPreset) {
    private val values = mutableStateMapOf<String, Any>()

    /** Incremented on every change; read during draw to trigger a redraw (not a recomposition). */
    var version by mutableIntStateOf(0)
        private set

    /** Source image for image-based shaders. `null` falls back to the bundled sample. */
    var image by mutableStateOf<Bitmap?>(null)

    var presetName by mutableStateOf(preset.name)
        private set

    init {
        applyPreset(preset)
    }

    fun applyPreset(preset: Preset) {
        values.clear()
        values.putAll(DEFAULTS)
        values.putAll(spec.defaults)
        values.putAll(spec.defaultPreset.values)
        values.putAll(preset.values)
        presetName = preset.name
        version++
    }

    operator fun get(key: String): Any? = values[key]

    fun float(key: String, fallback: Float = 0f): Float = (values[key] as? Float) ?: fallback

    fun bool(key: String): Boolean = values[key] as? Boolean ?: false

    fun option(key: String): String? = values[key] as? String

    fun color(key: String): ArgbColor? = values[key] as? ArgbColor

    @Suppress("UNCHECKED_CAST")
    fun colors(key: String): List<ArgbColor> = values[key] as? List<ArgbColor> ?: emptyList()

    fun set(key: String, value: Any) {
        if (values[key] == value) return
        values[key] = value
        version++
    }

    /** Snapshot of the current values, e.g. to copy as a new preset. */
    fun snapshot(): Map<String, Any> = values.toMap()

    companion object {
        private val DEFAULTS: Map<String, Any> = mapOf(
            "fit" to "contain",
            "scale" to 1f,
            "rotation" to 0f,
            "offsetX" to 0f,
            "offsetY" to 0f,
            "originX" to 0.5f,
            "originY" to 0.5f,
            "worldWidth" to 0f,
            "worldHeight" to 0f,
            "speed" to 0f,
            "frame" to 0f,
        )
    }
}
