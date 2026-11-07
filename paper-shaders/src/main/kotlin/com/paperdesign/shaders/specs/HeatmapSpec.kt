package com.paperdesign.shaders.specs

import com.paperdesign.shaders.*

/** Port of paper-design/shaders `heatmap` (Apache-2.0). Params and presets transcribed from upstream. */
val HeatmapSpec = ShaderSpec(
    id = "heatmap",
    title = "Heatmap",
    description = "A glowing gradient of colors flowing through an input shape. The effect creates " +
        "a smoothly animated wave of intensity across the image.",
    asset = "shaders/heatmap.agsl",
    animated = true,
    sizing = SizingKind.IMAGE,
    usesImage = true,
    params = listOf(
        ColorsParam("colors", "Colors", "u_colors", "u_colorsCount", maxCount = 10),
        ColorParam("colorBack", "Color back", "u_colorBack"),
        FloatParam("contour", "Contour", "u_contour", 0.0f, 1.0f),
        FloatParam("angle", "Angle", "u_angle", 0.0f, 360.0f),
        FloatParam("noise", "Noise", "u_noise", 0.0f, 1.0f),
        FloatParam("innerGlow", "Inner glow", "u_innerGlow", 0.0f, 1.0f),
        FloatParam("outerGlow", "Outer glow", "u_outerGlow", 0.0f, 1.0f),
    ),
    sampleImage = SampleImage.SHAPE,
    preprocess = ImagePreprocess.HEATMAP,
    presets = listOf(
        Preset(
            "Default",
            mapOf(
                "fit" to "contain",
                "scale" to 0.75f,
                "rotation" to 0.0f,
                "offsetX" to 0.0f,
                "offsetY" to 0.0f,
                "originX" to 0.5f,
                "originY" to 0.5f,
                "worldWidth" to 0.0f,
                "worldHeight" to 0.0f,
                "speed" to 1.0f,
                "frame" to 0.0f,
                "contour" to 0.5f,
                "angle" to 0.0f,
                "noise" to 0.0f,
                "innerGlow" to 0.5f,
                "outerGlow" to 0.5f,
                "colorBack" to color(0xff000000L),
                "colors" to colors(0xff11206aL, 0xff1f3ba2L, 0xff2f63e7L, 0xff6bd7ffL, 0xffffe679L, 0xffff991eL, 0xffff4c00L),
            ),
        ),
        Preset(
            "Sepia",
            mapOf(
                "fit" to "contain",
                "scale" to 0.75f,
                "rotation" to 0.0f,
                "offsetX" to 0.0f,
                "offsetY" to 0.0f,
                "originX" to 0.5f,
                "originY" to 0.5f,
                "worldWidth" to 0.0f,
                "worldHeight" to 0.0f,
                "speed" to 0.5f,
                "frame" to 0.0f,
                "contour" to 0.5f,
                "angle" to 0.0f,
                "noise" to 0.75f,
                "innerGlow" to 0.5f,
                "outerGlow" to 0.5f,
                "colorBack" to color(0xff000000L),
                "colors" to colors(0xff997F45L, 0xffffffffL),
            ),
        ),
    ),
)
