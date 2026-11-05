package com.paperdesign.shaders

import com.paperdesign.shaders.specs.ShaderCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Static consistency checks between the Kotlin specs and the AGSL assets. */
class ShaderCatalogTest {

    private val assets = File("src/main/assets")
    private val commonKeys = (CommonParams.sizing + CommonParams.speed + CommonParams.frame).map { it.key }.toSet()

    @Test
    fun catalog_containsAllThirtyPaperShaders() {
        assertEquals(30, ShaderCatalog.all.size)
        assertEquals(ShaderCatalog.all.size, ShaderCatalog.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyParamUniform_isDeclaredInItsShader() {
        for (spec in ShaderCatalog.all) {
            val source = ShaderPrelude.build(File(assets, spec.asset).readText())
            for (p in spec.params) {
                assertTrue("${spec.id}: ${p.uniform} not declared", Regex("uniform\\s+\\w+\\s+${p.uniform}\\b").containsMatchIn(source))
            }
        }
    }

    @Test
    fun shaders_useNoGlslOnlyFeatures() {
        val banned = listOf("fwidth(", "dFdx(", "dFdy(", "texture(", "textureLod(", "textureGrad(", "textureSize(", "#define", "sampler2D", "gl_FragCoord", "transpose(")
        for (spec in ShaderCatalog.all) {
            val code = File(assets, spec.asset).readLines().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")
            for (b in banned) assertTrue("${spec.id} uses $b", !code.contains(b))
            assertTrue("${spec.id} has an unresolved template", !code.contains("\${"))
        }
    }

    @Test
    fun presets_onlyUseKnownKeysAndValidValues() {
        for (spec in ShaderCatalog.all) {
            val byKey = spec.params.associateBy { it.key }
            for (preset in spec.presets) {
                for ((key, value) in preset.values) {
                    val p = byKey[key]
                    assertTrue("${spec.id}/${preset.name}: unknown key $key", p != null || key in commonKeys)
                    when (p) {
                        is EnumParam -> assertTrue("${spec.id}/${preset.name}: bad option $value", value in p.options)
                        is ColorsParam -> assertTrue("${spec.id}/${preset.name}: too many colors", (value as List<*>).size <= p.maxCount)
                        is ColorParam -> assertTrue(value is ArgbColor)
                        is FloatParam -> assertTrue(value is Float)
                        is BoolParam -> assertTrue(value is Boolean)
                        null -> Unit
                    }
                }
            }
        }
    }
}
