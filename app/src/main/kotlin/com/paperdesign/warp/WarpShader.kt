package com.paperdesign.warp

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import org.intellij.lang.annotations.Language

/**
 * Android AGSL (SkSL) implementation of Paper Design's Warp Shader.
 *
 * Source reference: https://shaders.paper.design/warp
 * Matches the mathematical fluid warp, layered vortex swirls, and geometric
 * shape distortion (Checks, Stripes, Edge) of the original WebGL shader.
 */
object WarpShader {

    @Language("AGSL")
    const val WARP_AGSL_SRC: String = """
        uniform float2 u_resolution;
        uniform float u_time;
        uniform float4 u_colors[10];
        uniform float u_colorsCount;
        uniform float u_proportion;
        uniform float u_softness;
        uniform float u_shape;
        uniform float u_shapeScale;
        uniform float u_distortion;
        uniform float u_swirl;
        uniform float u_swirlIterations;
        uniform float u_scale;
        uniform float u_rotation;
        uniform float2 u_offset;

        const float TWO_PI = 6.28318530718;
        const float PI = 3.14159265358979323846;

        // High-performance procedural hash producing continuous value noise
        float hash21(float2 p) {
            p = fract(p * float2(0.3183099, 0.3678794)) + 0.1;
            p += dot(p, p + 19.19);
            return fract(p.x * p.y);
        }

        float valueNoise(float2 st) {
            float2 i = floor(st);
            float2 f = fract(st);
            float a = hash21(i);
            float b = hash21(i + float2(1.0, 0.0));
            float c = hash21(i + float2(0.0, 1.0));
            float d = hash21(i + float2(1.0, 1.0));
            float2 u = f * f * (3.0 - 2.0 * f);
            return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
        }

        half4 main(float2 fragCoord) {
            float2 centered = fragCoord - 0.5 * u_resolution;
            float rad = u_rotation * PI / 180.0;
            float cosR = cos(rad);
            float sinR = sin(rad);
            float2 rotated = float2(cosR * centered.x - sinR * centered.y, sinR * centered.x + cosR * centered.y);

            float minDim = min(u_resolution.x, u_resolution.y);
            float2 uv = (rotated / max(u_scale, 0.001) + float2(-u_offset.x, u_offset.y) * minDim) * (5.0 / max(minDim, 1.0));
            uv *= 0.5;

            const float firstFrameOffset = 118.0;
            float t = 0.0625 * (u_time + firstFrameOffset);

            // Noise-based domain warping
            float n1 = valueNoise(uv * 1.0 + t);
            float n2 = valueNoise(uv * 2.0 - t);
            float angle = n1 * TWO_PI;
            uv.x += 4.0 * u_distortion * n2 * cos(angle);
            uv.y += 4.0 * u_distortion * n2 * sin(angle);

            // Layered swirl passes
            float swirl = u_swirl;
            for (int i = 1; i <= 20; i++) {
                if (float(i) >= u_swirlIterations) break;
                float iFloat = float(i);
                uv.x += swirl / iFloat * cos(t + iFloat * 1.5 * uv.y);
                uv.y += swirl / iFloat * cos(t + iFloat * 1.0 * uv.x);
            }

            float proportion = clamp(u_proportion, 0.0, 1.0);

            // Base geometric shape pattern
            float shape = 0.0;
            if (u_shape < 0.5) {
                // Checks
                float2 checksShape_uv = uv * (0.5 + 3.5 * u_shapeScale);
                shape = 0.5 + 0.5 * sin(checksShape_uv.x) * cos(checksShape_uv.y);
                shape += 0.48 * sign(proportion - 0.5) * pow(abs(proportion - 0.5), 0.5);
            } else if (u_shape < 1.5) {
                // Stripes
                float2 stripesShape_uv = uv * (2.0 * u_shapeScale);
                float f = fract(stripesShape_uv.y);
                shape = smoothstep(0.0, 0.55, f) * (1.0 - smoothstep(0.45, 1.0, f));
                shape += 0.48 * sign(proportion - 0.5) * pow(abs(proportion - 0.5), 0.5);
            } else {
                // Edge
                float shapeScaling = 5.0 * (1.0 - u_shapeScale);
                float e0 = 0.45 - shapeScaling;
                float e1 = 0.55 + shapeScaling;
                shape = smoothstep(min(e0, e1), max(e0, e1), 1.0 - uv.y + 0.3 * (proportion - 0.5));
            }

            // Multi-stop color blending with anti-aliased softness transitions
            float mixer = shape * (u_colorsCount - 1.0);
            float4 gradient = u_colors[0];
            gradient.rgb *= gradient.a;
            const float aa = 0.005;

            for (int i = 1; i < 10; i++) {
                if (float(i) >= u_colorsCount) break;
                float m = clamp(mixer - float(i - 1), 0.0, 1.0);

                float localMixerStart = floor(m);
                float softness = 0.5 * u_softness + 0.005;
                float smoothed = smoothstep(max(0.0, 0.5 - softness - aa), min(1.0, 0.5 + softness + aa), m - localMixerStart);
                float stepped = localMixerStart + smoothed;

                m = mix(stepped, m, u_softness);

                float4 c = u_colors[i];
                c.rgb *= c.a;
                gradient = mix(gradient, c, m);
            }

            float3 color = gradient.rgb;
            float opacity = gradient.a;

            // Anti-banding dither
            color += (1.0 / 256.0) * (fract(sin(dot(0.014 * fragCoord.xy, float2(12.9898, 78.233))) * 43758.5453123) - 0.5);

            return half4(color, opacity);
        }
    """

    /**
     * Creates a new [RuntimeShader] instance compiled with the Warp AGSL source.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createRuntimeShader(): RuntimeShader = RuntimeShader(WARP_AGSL_SRC)

    /**
     * Updates all uniforms on a [RuntimeShader] in place.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun updateUniforms(
        shader: RuntimeShader,
        width: Float,
        height: Float,
        timeSeconds: Float,
        params: WarpParams,
        colorBuffer: FloatArray = params.toColorFloatArray()
    ) {
        shader.setFloatUniform("u_resolution", width, height)
        shader.setFloatUniform("u_time", timeSeconds)
        shader.setFloatUniform("u_colors", colorBuffer)
        shader.setFloatUniform("u_colorsCount", minOf(params.colors.size, 10).toFloat())
        shader.setFloatUniform("u_proportion", params.proportion)
        shader.setFloatUniform("u_softness", params.softness)
        shader.setFloatUniform("u_shape", params.shape.shaderValue)
        shader.setFloatUniform("u_shapeScale", params.shapeScale)
        shader.setFloatUniform("u_distortion", params.distortion)
        shader.setFloatUniform("u_swirl", params.swirl)
        shader.setFloatUniform("u_swirlIterations", params.swirlIterations.toFloat())
        shader.setFloatUniform("u_scale", params.scale)
        shader.setFloatUniform("u_rotation", params.rotation)
        shader.setFloatUniform("u_offset", params.offsetX, params.offsetY)
    }
}
