package com.paperdesign.shaders.specs

import com.paperdesign.shaders.ShaderSpec

/** Every ported shader, in gallery order. */
object ShaderCatalog {
    /** Original interactive shaders made for this app. */
    val originals: List<ShaderSpec> = listOf(
        RipplePondSpec,
        GravityGooSpec,
        FlowLinesSpec,
    )

    /** Ports of paper-design/shaders. */
    val paper: List<ShaderSpec> = listOf(
        ColorPanelsSpec,
        DitheringSpec,
        DotGridSpec,
        DotOrbitSpec,
        GodRaysSpec,
        GrainGradientSpec,
        MeshGradientSpec,
        MetaballsSpec,
        NeuroNoiseSpec,
        PerlinNoiseSpec,
        PulsingBorderSpec,
        SimplexNoiseSpec,
        SmokeRingSpec,
        SpiralSpec,
        StaticMeshGradientSpec,
        StaticRadialGradientSpec,
        SwirlSpec,
        VoronoiSpec,
        WarpSpec,
        WavesSpec,
        FlutedGlassSpec,
        HalftoneCmykSpec,
        HalftoneDotsSpec,
        ImageDitheringSpec,
        LensDistortionSpec,
        PaperTextureSpec,
        WaterSpec,
        HeatmapSpec,
        GemSmokeSpec,
        LiquidMetalSpec,
    )

    /** [originals] followed by [paper]. Ids are unique across the list. */
    val all: List<ShaderSpec> = originals + paper

    private val byIdMap: Map<String, ShaderSpec> by lazy { all.associateBy { it.id } }

    /** The shader with this id (e.g. `"warp"`, `"mesh-gradient"`), or null if there is none. */
    fun byId(id: String): ShaderSpec? = byIdMap[id]

    /** Like [byId] but throws [IllegalArgumentException] naming the valid ids. */
    fun require(id: String): ShaderSpec = byId(id)
        ?: throw IllegalArgumentException("Unknown shader id \"$id\". Valid ids: ${all.joinToString { it.id }}")
}
