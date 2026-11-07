package com.paperdesign.shaders

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

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

    /** Name of the last applied preset. Edits do not change it. */
    var presetName by mutableStateOf(preset.name)
        private set

    init {
        applyPreset(preset)
    }

    /**
     * Resets every value, then applies, in order: the common defaults, [ShaderSpec.defaults], the
     * spec's default preset, and [preset]. Keys a preset leaves out therefore keep the default
     * preset's value, as upstream does.
     */
    fun applyPreset(preset: Preset) {
        values.clear()
        values.putAll(DEFAULTS)
        values.putAll(spec.defaults)
        values.putAll(spec.defaultPreset.values)
        values.putAll(preset.values)
        presetName = preset.name
        version++
    }

    /** Applies the preset named [name]. Throws [IllegalArgumentException] if the spec has none by that name. */
    fun applyPreset(name: String) {
        applyPreset(
            spec.preset(name)
                ?: throw IllegalArgumentException("${spec.id} has no preset \"$name\". Presets: ${spec.presets.joinToString { it.name }}"),
        )
    }

    /** The raw value for [key], or null when unset. Prefer the typed getters below. */
    operator fun get(key: String): Any? = values[key]

    /** A [FloatParam] value (also `speed`, `frame` and the sizing numbers), or [fallback]. */
    fun float(key: String, fallback: Float = 0f): Float = (values[key] as? Float) ?: fallback

    /** A [BoolParam] value; false when unset. */
    fun bool(key: String): Boolean = values[key] as? Boolean ?: false

    /** The selected option of an [EnumParam] (e.g. `fit` is `"contain"`), or null. */
    fun option(key: String): String? = values[key] as? String

    /** A [ColorParam] value, or null. */
    fun color(key: String): ArgbColor? = values[key] as? ArgbColor

    /** A [ColorsParam] value; empty when unset. */
    @Suppress("UNCHECKED_CAST")
    fun colors(key: String): List<ArgbColor> = values[key] as? List<ArgbColor> ?: emptyList()

    /**
     * Sets one parameter. Cheap: only bumps [version], and the renderer re-uploads uniforms on
     * the next frame without recomposing.
     *
     * The value is checked against the parameter's type so that mistakes fail loudly instead of
     * silently rendering a default:
     * - [FloatParam] and common numbers: any [Number] (stored as Float).
     * - [EnumParam]: one of [EnumParam.options] (a String).
     * - [BoolParam]: Boolean. [ColorParam]: [ArgbColor] or Compose `Color`.
     * - [ColorsParam]: a non-empty list of up to [ColorsParam.maxCount] [ArgbColor] or Compose `Color`.
     *
     * Keys that are not parameters of [spec] (see [ShaderSpec.param]) are only accepted when the
     * spec lists them in [ShaderSpec.defaults].
     *
     * @throws IllegalArgumentException for an unknown key or a value of the wrong type.
     */
    fun set(key: String, value: Any) {
        val normalized = normalize(key, value)
        if (values[key] == normalized) return
        values[key] = normalized
        version++
    }

    private fun normalize(key: String, value: Any): Any {
        fun bad(expected: String): Nothing =
            throw IllegalArgumentException("${spec.id}.$key expects $expected, got ${value::class.simpleName} ($value)")
        fun asArgb(v: Any?): ArgbColor? = when (v) {
            is ArgbColor -> v
            is Color -> v.toArgbColor()
            else -> null
        }
        return when (val p = spec.param(key)) {
            is FloatParam -> (value as? Number)?.toFloat() ?: bad("a number")
            is EnumParam -> (value as? String)?.takeIf { it in p.options } ?: bad("one of ${p.options}")
            is BoolParam -> value as? Boolean ?: bad("a Boolean")
            is ColorParam -> asArgb(value) ?: bad("an ArgbColor or Color")
            is ColorsParam -> {
                val list = (value as? List<*>)?.map { asArgb(it) ?: bad("a list of ArgbColor or Color") } ?: bad("a list of colors")
                if (list.isEmpty() || list.size > p.maxCount) bad("1 to ${p.maxCount} colors")
                list
            }
            null -> if (key in spec.defaults) value else throw IllegalArgumentException(
                "${spec.id} has no parameter \"$key\". Parameters: ${spec.allParams.joinToString { it.key }}",
            )
        }
    }

    /** Snapshot of the current values, e.g. to copy as a new preset. */
    fun snapshot(): Map<String, Any> = values.toMap()

    companion object {
        internal val DEFAULTS: Map<String, Any> = mapOf(
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
