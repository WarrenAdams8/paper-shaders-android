package com.paperdesign.shaders.specs

import com.paperdesign.shaders.ShaderSpec

/** Every ported shader, in gallery order. */
object ShaderCatalog {
    val all: List<ShaderSpec> = listOf(
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
    )
}
