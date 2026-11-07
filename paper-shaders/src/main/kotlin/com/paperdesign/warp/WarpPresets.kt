package com.paperdesign.warp

import androidx.compose.ui.graphics.Color

/**
 * Standard preset configurations from Paper Design Warp shader.
 */
object WarpPresets {

    /**
     * The signature purple/dark fluid checkerboard warp from shaders.paper.design/warp.
     */
    val Default = WarpParams(
        colors = listOf(
            Color(0xFF121212),
            Color(0xFF9470FF),
            Color(0xFF121212),
            Color(0xFF8838FF)
        ),
        proportion = 0.45f,
        softness = 1.0f,
        distortion = 0.25f,
        swirl = 0.8f,
        swirlIterations = 10,
        shape = WarpShape.Checks,
        shapeScale = 0.1f,
        speed = 1.0f,
        scale = 1.0f,
        rotation = 0.0f
    )

    /**
     * Cauldron Pot preset: mysterious green and deep blue swirling liquid with split-edge pattern.
     */
    val CauldronPot = WarpParams(
        colors = listOf(
            Color(0xFFA7E58B),
            Color(0xFF324472),
            Color(0xFF0A180D)
        ),
        proportion = 0.64f,
        softness = 1.0f,
        distortion = 0.2f,
        swirl = 0.86f,
        swirlIterations = 7,
        shape = WarpShape.Edge,
        shapeScale = 0.6f,
        scale = 0.9f,
        rotation = 160.0f,
        speed = 2.0f
    )

    /**
     * Live Ink preset: crisp monochromatic marbled ink strokes.
     */
    val LiveInk = WarpParams(
        colors = listOf(
            Color(0xFF111314),
            Color(0xFF9FAEAB),
            Color(0xFFF3FEE7),
            Color(0xFFF3FEE7)
        ),
        proportion = 0.05f,
        softness = 0.0f,
        distortion = 0.25f,
        swirl = 0.8f,
        swirlIterations = 10,
        shape = WarpShape.Checks,
        shapeScale = 0.28f,
        scale = 1.2f,
        rotation = 44.0f,
        offsetY = -0.3f,
        speed = 1.5f
    )

    /**
     * Kelp preset: flowing aquatic stripes of green and deep ocean hues.
     */
    val Kelp = WarpParams(
        colors = listOf(
            Color(0xFFDBFF8F),
            Color(0xFF404F3E),
            Color(0xFF091316)
        ),
        proportion = 0.67f,
        softness = 0.0f,
        distortion = 0.0f,
        swirl = 0.2f,
        swirlIterations = 3,
        shape = WarpShape.Stripes,
        shapeScale = 1.0f,
        scale = 0.8f,
        rotation = 50.0f,
        speed = 3.0f
    )

    /**
     * Nectar preset: warm honey, gold, and cream fluid gradients.
     */
    val Nectar = WarpParams(
        colors = listOf(
            Color(0xFF151310),
            Color(0xFFD3A86B),
            Color(0xFFF0EDEA)
        ),
        proportion = 0.24f,
        softness = 1.0f,
        distortion = 0.21f,
        swirl = 0.57f,
        swirlIterations = 10,
        shape = WarpShape.Edge,
        shapeScale = 0.75f,
        scale = 2.0f,
        offsetY = 0.6f,
        speed = 2.0f
    )

    /**
     * Passion preset: rich crimson, rose, and peach swirling vortex.
     */
    val Passion = WarpParams(
        colors = listOf(
            Color(0xFF3B1515),
            Color(0xFF954751),
            Color(0xFFFFC085)
        ),
        proportion = 0.5f,
        softness = 1.0f,
        distortion = 0.09f,
        swirl = 0.9f,
        swirlIterations = 6,
        shape = WarpShape.Checks,
        shapeScale = 0.25f,
        scale = 2.5f,
        rotation = 1.35f,
        speed = 1.8f
    )

    val AllPresets: List<Pair<String, WarpParams>> = listOf(
        "Default" to Default,
        "Cauldron Pot" to CauldronPot,
        "Live Ink" to LiveInk,
        "Kelp" to Kelp,
        "Nectar" to Nectar,
        "Passion" to Passion
    )
}
