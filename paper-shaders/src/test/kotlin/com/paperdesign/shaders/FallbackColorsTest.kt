package com.paperdesign.shaders

import com.paperdesign.shaders.specs.ShaderCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pre-API 33 gradient colors that both the Modifier and PaperShaderView draw. */
class FallbackColorsTest {

    @Test
    fun everyShader_hasAtLeastOneFallbackColor() {
        for (spec in ShaderCatalog.all) {
            assertTrue(spec.id, fallbackColors(ShaderState(spec)).isNotEmpty())
        }
    }

    @Test
    fun colorParams_areCollectedInSpecOrder() {
        for (spec in ShaderCatalog.all) {
            val state = ShaderState(spec)
            val expected = spec.params.flatMap { p ->
                when (p) {
                    is ColorsParam -> state.colors(p.key)
                    is ColorParam -> listOfNotNull(state.color(p.key))
                    else -> emptyList()
                }
            }
            if (expected.isNotEmpty()) assertEquals(spec.id, expected, fallbackColors(state))
        }
    }

    @Test
    fun editingAColor_changesTheFallback() {
        val spec = ShaderCatalog.all.first { s -> s.params.any { it is ColorsParam } }
        val key = spec.params.first { it is ColorsParam }.key
        val state = ShaderState(spec)
        state.set(key, listOf(ArgbColor(0xFF112233)))
        assertEquals(ArgbColor(0xFF112233), fallbackColors(state).first())
    }

    @Test
    fun catalogIds_resolveForXmlShaderAttribute() {
        // PaperShaderView looks shaders up by id; the demo layout uses these.
        for (id in listOf("mesh-gradient", "dot-orbit", "halftone-dots", "swirl")) {
            assertTrue(id, ShaderCatalog.all.any { it.id == id })
        }
    }
}
