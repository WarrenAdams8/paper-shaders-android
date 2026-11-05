package com.paperdesign.warp

/**
 * Geometric base patterns supported by the Warp shader.
 */
enum class WarpShape(val shaderValue: Float) {
    /** Checkerboard grid pattern */
    Checks(0f),

    /** Linear striped bands pattern */
    Stripes(1f),

    /** Soft split-edge gradient pattern */
    Edge(2f);

    companion object {
        fun fromValue(value: Float): WarpShape = when {
            value < 0.5f -> Checks
            value < 1.5f -> Stripes
            else -> Edge
        }
    }
}
