package com.paperdesign.shaders

import androidx.compose.runtime.Immutable

/** How the upstream vertex shader sizes this shader's UV space. */
enum class SizingKind { OBJECT, PATTERN, IMAGE }

/** A single user-editable parameter of a shader, and the AGSL uniform it drives. */
@Immutable
sealed interface ParamDef {
    val key: String
    val label: String
    val uniform: String
}

/** A continuous (or stepped, when [step] > 0) number. */
@Immutable
data class FloatParam(
    override val key: String,
    override val label: String,
    override val uniform: String,
    val min: Float,
    val max: Float,
    val step: Float = 0f,
) : ParamDef

/** A named option mapped to the float value the shader expects. */
@Immutable
class EnumParam(
    override val key: String,
    override val label: String,
    override val uniform: String,
    val options: List<String>,
    val values: FloatArray,
) : ParamDef {
    fun valueOf(option: String): Float = values[options.indexOf(option).coerceAtLeast(0)]
}

/** A boolean, uploaded as 0.0 / 1.0 (AGSL has no bool uniforms). */
@Immutable
data class BoolParam(
    override val key: String,
    override val label: String,
    override val uniform: String,
) : ParamDef

/** A single RGBA color (`float4`). */
@Immutable
data class ColorParam(
    override val key: String,
    override val label: String,
    override val uniform: String,
) : ParamDef

/** An array of up to [maxCount] RGBA colors plus a count uniform. */
@Immutable
data class ColorsParam(
    override val key: String,
    override val label: String,
    override val uniform: String,
    val countUniform: String,
    val maxCount: Int,
) : ParamDef

/** A named set of parameter values, as published by Paper. */
@Immutable
data class Preset(val name: String, val values: Map<String, Any>)

/** A color stored as ARGB. Wrapped so presets can tell colors from numbers. */
@JvmInline
value class ArgbColor(val argb: Long)

fun color(argb: Long) = ArgbColor(argb)

fun colors(vararg argb: Long): List<ArgbColor> = argb.map { ArgbColor(it) }

/** Everything needed to render, describe and edit one Paper shader. */
@Immutable
data class ShaderSpec(
    val id: String,
    val title: String,
    val description: String,
    /** Path of the AGSL body inside `assets/`. */
    val asset: String,
    val animated: Boolean,
    val sizing: SizingKind,
    val usesImage: Boolean,
    val params: List<ParamDef>,
    val presets: List<Preset>,
    /** Values applied before any preset (for params that upstream derives from props). */
    val defaults: Map<String, Any> = emptyMap(),
    /** Which bundled image an image shader starts with. */
    val sampleImage: SampleImage = SampleImage.PHOTO,
    /** CPU preprocessing upstream runs on the image before upload, if any. */
    val preprocess: ImagePreprocess? = null,
) {
    val defaultPreset: Preset get() = presets.first()
}

enum class SampleImage { PHOTO, SHAPE }

/** Upstream `toProcessed*` image preprocessing steps, ported in [ImageProcessing]. */
enum class ImagePreprocess(val run: (android.graphics.Bitmap) -> android.graphics.Bitmap) {
    HEATMAP({ ImageProcessing.heatmap(it) }),
    GEM_SMOKE({ ImageProcessing.poissonRoundness(it, padding = 0.025f, iterations = 32, smoothingPasses = 3) }),
    LIQUID_METAL({ ImageProcessing.poissonRoundness(it, padding = 0f, iterations = 40, smoothingPasses = 0) }),
}

/** Sizing and motion parameters every shader shares (the upstream "common params"). */
object CommonParams {
    val fit = EnumParam("fit", "Fit", "u_fit", listOf("none", "contain", "cover"), floatArrayOf(0f, 1f, 2f))
    val scale = FloatParam("scale", "Scale", "u_scale", 0.01f, 4f)
    val rotation = FloatParam("rotation", "Rotation", "u_rotation", 0f, 360f)
    val offsetX = FloatParam("offsetX", "Offset X", "u_offsetX", -1f, 1f)
    val offsetY = FloatParam("offsetY", "Offset Y", "u_offsetY", -1f, 1f)
    val originX = FloatParam("originX", "Origin X", "u_originX", 0f, 1f)
    val originY = FloatParam("originY", "Origin Y", "u_originY", 0f, 1f)
    val worldWidth = FloatParam("worldWidth", "World width", "u_worldWidth", 0f, 2000f, step = 1f)
    val worldHeight = FloatParam("worldHeight", "World height", "u_worldHeight", 0f, 2000f, step = 1f)
    val speed = FloatParam("speed", "Speed", "", 0f, 10f)
    val frame = FloatParam("frame", "Frame (ms)", "", 0f, 100_000f, step = 1f)

    val sizing: List<ParamDef> = listOf(fit, scale, rotation, offsetX, offsetY, originX, originY, worldWidth, worldHeight)

    fun forSpec(spec: ShaderSpec): List<ParamDef> {
        val motion = if (spec.animated) listOf(speed, frame) else emptyList()
        return motion + sizing
    }
}
