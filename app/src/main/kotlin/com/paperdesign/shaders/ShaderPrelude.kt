package com.paperdesign.shaders

/**
 * Builds the AGSL header for a shader body.
 *
 * Shader bodies (in `assets/shaders/`) are the upstream fragment shaders converted to AGSL.
 * They call `paper_objectUV(fragCoord)` etc. in place of the upstream vertex varyings, and
 * `paper_noise(uv)` / `paper_image(uv)` in place of `texture()`. This prelude declares only
 * the uniforms and helpers a body actually references, so every declared uniform gets set.
 */
object ShaderPrelude {

    private class Provided(val symbol: String, val code: String)

    private val provided = listOf(
        Provided(
            "paper_objectUV",
            """
            uniform float3 u_objectUVx;
            uniform float3 u_objectUVy;
            float2 paper_objectUV(float2 p) { return float2(dot(u_objectUVx, float3(p, 1.)), dot(u_objectUVy, float3(p, 1.))); }
            """,
        ),
        Provided(
            "paper_responsiveUV",
            """
            uniform float3 u_responsiveUVx;
            uniform float3 u_responsiveUVy;
            float2 paper_responsiveUV(float2 p) { return float2(dot(u_responsiveUVx, float3(p, 1.)), dot(u_responsiveUVy, float3(p, 1.))); }
            """,
        ),
        Provided(
            "paper_patternUV",
            """
            uniform float3 u_patternUVx;
            uniform float3 u_patternUVy;
            float2 paper_patternUV(float2 p) { return float2(dot(u_patternUVx, float3(p, 1.)), dot(u_patternUVy, float3(p, 1.))); }
            """,
        ),
        Provided(
            "paper_imageUV",
            """
            uniform float3 u_imageUVx;
            uniform float3 u_imageUVy;
            float2 paper_imageUV(float2 p) { return float2(dot(u_imageUVx, float3(p, 1.)), dot(u_imageUVy, float3(p, 1.))); }
            """,
        ),
        Provided("paper_objectBoxSize", "uniform float2 u_objectBoxSize;\nfloat2 paper_objectBoxSize(float2 p) { return u_objectBoxSize; }"),
        Provided("paper_patternBoxSize", "uniform float2 u_patternBoxSize;\nfloat2 paper_patternBoxSize(float2 p) { return u_patternBoxSize; }"),
        Provided(
            "paper_responsiveBoxGivenSize",
            "uniform float2 u_responsiveBoxGivenSize;\nfloat2 paper_responsiveBoxGivenSize(float2 p) { return u_responsiveBoxGivenSize; }",
        ),
        Provided("u_pointer", "uniform float3 u_pointer;"),
        Provided("u_tilt", "uniform float2 u_tilt;"),
        Provided("u_touches", "uniform float4 u_touches[8];"),
        Provided("u_objectPx", "uniform float u_objectPx;"),
        Provided("u_responsivePx", "uniform float u_responsivePx;"),
        Provided("u_patternPx", "uniform float u_patternPx;"),
        Provided("u_imagePx", "uniform float u_imagePx;"),
        Provided(
            "paper_round",
            """
            // GLSL ES 3 round(); not available in AGSL.
            float paper_round(float x) { return floor(x + .5); }
            float2 paper_round(float2 x) { return floor(x + .5); }
            float3 paper_round(float3 x) { return floor(x + .5); }
            """,
        ),
        Provided(
            "paper_noise",
            """
            uniform shader u_noiseTexture;
            // Upstream samples a 128x128 texture with CLAMP_TO_EDGE + LINEAR filtering.
            float4 paper_noise(float2 uv) { return float4(u_noiseTexture.eval(uv * 128.)); }
            """,
        ),
        Provided(
            "paper_image",
            """
            uniform shader u_image;
            uniform float2 u_imageSize;
            float4 paper_image(float2 uv) { return float4(u_image.eval(uv * u_imageSize)); }
            """,
        ),
    )

    private val header = """
        uniform float2 u_resolution;
        uniform float u_pixelRatio;
    """.trimIndent()

    fun build(body: String): String {
        val sb = StringBuilder(header.length + body.length + 2048)
        sb.append(header).append('\n')
        for (p in provided) {
            if (body.contains(p.symbol)) sb.append(p.code.trimIndent()).append('\n')
        }
        sb.append(body)
        return sb.toString()
    }
}
