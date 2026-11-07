package com.paperdesign.shaders

import androidx.compose.ui.graphics.Color
import com.paperdesign.shaders.specs.ShaderCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Catalog lookups, spec metadata and color conversions that the public API relies on. */
class ShaderCatalogLookupTest {

    @Test
    fun byId_findsEveryShader() {
        for (spec in ShaderCatalog.all) assertSame(spec, ShaderCatalog.byId(spec.id))
        assertNull(ShaderCatalog.byId("not-a-shader"))
    }

    @Test
    fun require_listsValidIdsOnFailure() {
        val e = assertThrows(IllegalArgumentException::class.java) { ShaderCatalog.require("meshgradient") }
        assertTrue(e.message!!.contains("mesh-gradient"))
    }

    @Test
    fun ids_areKebabCaseAndMatchAssetNames() {
        for (spec in ShaderCatalog.all) {
            assertTrue(spec.id, Regex("[a-z0-9]+(-[a-z0-9]+)*").matches(spec.id))
            assertEquals(spec.id, "shaders/${spec.id}.agsl", spec.asset)
        }
    }

    @Test
    fun everyAssetFile_belongsToASpec() {
        val files = File("src/main/assets/shaders").list()!!.filter { it.endsWith(".agsl") }.toSet()
        val used = ShaderCatalog.all.map { it.asset.removePrefix("shaders/") }.toSet()
        assertEquals(used, files)
    }

    @Test
    fun everySpec_hasPresetsWithUniqueNames() {
        for (spec in ShaderCatalog.all) {
            assertTrue(spec.id, spec.presets.isNotEmpty())
            assertEquals(spec.id, spec.presets.size, spec.presets.map { it.name }.toSet().size)
            assertTrue(spec.id, spec.title.isNotBlank() && spec.description.isNotBlank())
        }
    }

    @Test
    fun paramKeys_areUniqueAndDoNotShadowCommonParams() {
        val common = CommonParams.sizing.map { it.key } + CommonParams.speed.key + CommonParams.frame.key
        for (spec in ShaderCatalog.all) {
            val keys = spec.params.map { it.key }
            assertEquals(spec.id, keys.size, keys.toSet().size)
            assertTrue("${spec.id} shadows a common param", keys.none { it in common })
        }
    }

    @Test
    fun floatParams_haveSaneRanges() {
        for (spec in ShaderCatalog.all) for (p in spec.allParams.filterIsInstance<FloatParam>()) {
            assertTrue("${spec.id}.${p.key}", p.min < p.max && p.step >= 0f)
        }
    }

    @Test
    fun enumParams_haveOneValuePerOption() {
        for (spec in ShaderCatalog.all) for (p in spec.allParams.filterIsInstance<EnumParam>()) {
            assertEquals("${spec.id}.${p.key}", p.options.size, p.values.size)
            assertEquals(p.values[0], p.valueOf("not an option"))
        }
    }

    @Test
    fun param_findsOwnAndCommonParams() {
        val spec = ShaderCatalog.require("warp")
        assertSame(spec.params.first(), spec.param(spec.params.first().key))
        assertSame(CommonParams.scale, spec.param("scale"))
        assertNull(spec.param("bogus"))
    }

    @Test
    fun motionParams_onlyForAnimatedShaders() {
        for (spec in ShaderCatalog.all) {
            assertEquals(spec.id, spec.animated, CommonParams.speed in spec.allParams)
        }
    }

    @Test
    fun argbColor_roundTripsThroughComposeColor() {
        for (argb in listOf(0xFF000000, 0xFFFFFFFF, 0x80FF8800, 0x00000000, 0xFF123456)) {
            val c = ArgbColor(argb)
            assertEquals(c, c.toColor().toArgbColor())
        }
        assertEquals(ArgbColor(0xFF00FF00), Color.Green.toArgbColor())
    }
}
