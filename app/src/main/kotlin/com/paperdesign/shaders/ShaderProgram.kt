package com.paperdesign.shaders

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * A compiled AGSL program for one [ShaderSpec] plus the logic that pushes a [ShaderState]
 * into its uniforms.
 *
 * Per frame only `u_time` is written. Everything else is uploaded when the state version,
 * the size or the image changes. No allocations happen on the per-frame path.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ShaderProgram(val spec: ShaderSpec, body: String) {
    val source: String = ShaderPrelude.build(body)
    val shader: RuntimeShader = RuntimeShader(source)

    private val declared: Set<String> = UNIFORM_REGEX.findAll(source).map { it.groupValues[2] }.toSet()
    private fun has(name: String) = name in declared

    private val hasTime = has("u_time")
    private val sizing = PaperSizing()
    private val color4 = FloatArray(4)
    private val colorArrays: Map<String, FloatArray> = spec.params.filterIsInstance<ColorsParam>()
        .associate { it.key to FloatArray(it.maxCount * 4) }

    private var uploadedVersion = -1
    private var uploadedW = -1f
    private var uploadedH = -1f
    private var uploadedImage: Bitmap? = null
    private var imageAspect = 1f

    init {
        if (has("u_noiseTexture")) {
            // Raw buffer sampling: the noise is data, so it must not be color managed.
            shader.setInputBuffer("u_noiseTexture", ShaderAssets.noiseShader)
        }
    }

    /**
     * Brings uniforms up to date. Cheap when nothing but time changed.
     * @param timeMs Paper's `currentFrame` (milliseconds of animation time).
     */
    fun update(state: ShaderState, width: Float, height: Float, density: Float, timeMs: Float, image: Bitmap?) {
        val version = state.version
        if (spec.usesImage && image !== uploadedImage && image != null) {
            uploadImage(image)
        }
        if (version != uploadedVersion || width != uploadedW || height != uploadedH) {
            uploadParams(state, width, height, density)
            uploadedVersion = version
            uploadedW = width
            uploadedH = height
        }
        if (hasTime) shader.setFloatUniform("u_time", timeMs * 0.001f)
    }

    private fun uploadImage(image: Bitmap) {
        val bs = BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        bs.filterMode = BitmapShader.FILTER_MODE_LINEAR
        shader.setInputShader("u_image", bs)
        imageAspect = image.width.toFloat() / image.height.toFloat()
        if (has("u_imageSize")) shader.setFloatUniform("u_imageSize", image.width.toFloat(), image.height.toFloat())
        if (has("u_imageAspectRatio")) shader.setFloatUniform("u_imageAspectRatio", imageAspect)
        uploadedImage = image
        uploadedVersion = -1 // image UVs depend on the aspect ratio
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun uploadParams(state: ShaderState, w: Float, h: Float, density: Float) {
        val fit = CommonParams.fit.valueOf(state.option("fit") ?: "none")
        val scale = state.float("scale", 1f)
        val rotation = state.float("rotation")
        val offsetX = state.float("offsetX")
        val offsetY = state.float("offsetY")
        val originX = state.float("originX", 0.5f)
        val originY = state.float("originY", 0.5f)
        val worldW = state.float("worldWidth")
        val worldH = state.float("worldHeight")

        setF("u_resolution", w, h)
        setF("u_pixelRatio", density)
        setF("u_fit", fit)
        setF("u_scale", scale)
        setF("u_rotation", rotation)
        setF("u_offsetX", offsetX)
        setF("u_offsetY", offsetY)
        setF("u_originX", originX)
        setF("u_originY", originY)
        setF("u_worldWidth", worldW)
        setF("u_worldHeight", worldH)
        setF("u_imageAspectRatio", imageAspect)

        sizing.compute(w, h, density, imageAspect, fit, scale, rotation, offsetX, offsetY, originX, originY, worldW, worldH)
        setRows("u_objectUV", sizing.objectUV)
        setRows("u_responsiveUV", sizing.responsiveUV)
        setRows("u_patternUV", sizing.patternUV)
        setRows("u_imageUV", sizing.imageUV)
        setF2("u_objectBoxSize", sizing.objectBoxSize)
        setF2("u_responsiveBoxGivenSize", sizing.responsiveBoxGivenSize)
        setF2("u_patternBoxSize", sizing.patternBoxSize)
        setF("u_objectPx", sizing.objectPx)
        setF("u_responsivePx", sizing.responsivePx)
        setF("u_patternPx", sizing.patternPx)
        setF("u_imagePx", sizing.imagePx)
        setF("u_isImage", 1f)

        for (p in spec.params) {
            if (!has(p.uniform)) continue
            when (p) {
                is FloatParam -> shader.setFloatUniform(p.uniform, state.float(p.key))
                is BoolParam -> shader.setFloatUniform(p.uniform, if (state.bool(p.key)) 1f else 0f)
                is EnumParam -> shader.setFloatUniform(p.uniform, p.valueOf(state.option(p.key) ?: p.options[0]))
                is ColorParam -> {
                    writeColor(state.color(p.key), color4, 0)
                    shader.setFloatUniform(p.uniform, color4)
                }
                is ColorsParam -> {
                    val arr = colorArrays.getValue(p.key)
                    val list = state.colors(p.key)
                    val n = minOf(list.size, p.maxCount)
                    arr.fill(0f)
                    for (i in 0 until n) writeColor(list[i], arr, i * 4)
                    shader.setFloatUniform(p.uniform, arr)
                    if (has(p.countUniform)) shader.setFloatUniform(p.countUniform, n.toFloat())
                }
            }
        }
    }

    private fun setF(name: String, v: Float) { if (has(name)) shader.setFloatUniform(name, v) }
    private fun setF(name: String, a: Float, b: Float) { if (has(name)) shader.setFloatUniform(name, a, b) }
    private fun setF2(name: String, v: FloatArray) { if (has(name)) shader.setFloatUniform(name, v[0], v[1]) }
    private fun setRows(base: String, m: FloatArray) {
        val x = base + "x"
        if (!has(x)) return
        shader.setFloatUniform(x, m[0], m[1], m[2])
        shader.setFloatUniform(base + "y", m[3], m[4], m[5])
    }

    private companion object {
        val UNIFORM_REGEX = Regex("""uniform\s+(\w+)\s+(\w+)""")

        /** Matches upstream `getShaderColorFromString`: straight sRGB components in 0..1. */
        fun writeColor(c: ArgbColor?, out: FloatArray, o: Int) {
            val v = c?.argb ?: 0L
            out[o] = ((v shr 16) and 0xFF) / 255f
            out[o + 1] = ((v shr 8) and 0xFF) / 255f
            out[o + 2] = (v and 0xFF) / 255f
            out[o + 3] = ((v shr 24) and 0xFF) / 255f
        }
    }
}
