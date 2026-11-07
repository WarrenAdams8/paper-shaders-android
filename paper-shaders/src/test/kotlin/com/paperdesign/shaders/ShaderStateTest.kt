package com.paperdesign.shaders

import androidx.compose.ui.graphics.Color
import com.paperdesign.shaders.specs.MeshGradientSpec
import com.paperdesign.shaders.specs.ShaderCatalog
import com.paperdesign.shaders.specs.WarpSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Editing, preset and validation behaviour of [ShaderState]. */
class ShaderStateTest {

    @Test
    fun newState_usesDefaultPresetValues() {
        val state = ShaderState(MeshGradientSpec)
        assertEquals(MeshGradientSpec.defaultPreset.name, state.presetName)
        for ((key, value) in MeshGradientSpec.defaultPreset.values) assertEquals(key, value, state[key])
    }

    @Test
    fun commonDefaults_areAlwaysPresent() {
        for (spec in ShaderCatalog.all) {
            val state = ShaderState(spec)
            for (p in CommonParams.sizing) assertTrue("${spec.id}.${p.key}", state[p.key] != null)
        }
    }

    @Test
    fun applyPreset_fallsBackToDefaultPresetForMissingKeys() {
        for (spec in ShaderCatalog.all) {
            for (preset in spec.presets) {
                val state = ShaderState(spec, preset)
                for ((key, value) in spec.defaultPreset.values) {
                    val expected = preset.values[key] ?: value
                    assertEquals("${spec.id}/${preset.name}.$key", expected, state[key])
                }
            }
        }
    }

    @Test
    fun applyPreset_discardsEarlierEdits() {
        val state = ShaderState(MeshGradientSpec)
        state.set("scale", 3f)
        state.applyPreset(MeshGradientSpec.defaultPreset)
        assertEquals(MeshGradientSpec.defaultPreset.values["scale"] ?: 1f, state.float("scale"))
    }

    @Test
    fun applyPresetByName_findsPresetOrThrows() {
        val last = WarpSpec.presets.last()
        val state = ShaderState(WarpSpec)
        state.applyPreset(last.name)
        assertEquals(last.name, state.presetName)
        val e = assertThrows(IllegalArgumentException::class.java) { state.applyPreset("No such preset") }
        assertTrue(e.message!!.contains(WarpSpec.defaultPreset.name))
    }

    @Test
    fun set_bumpsVersionOnlyWhenValueChanges() {
        val state = ShaderState(MeshGradientSpec)
        val v0 = state.version
        state.set("scale", 2f)
        assertEquals(v0 + 1, state.version)
        state.set("scale", 2f)
        assertEquals(v0 + 1, state.version)
    }

    @Test
    fun set_acceptsAnyNumberForFloatParams() {
        val state = ShaderState(MeshGradientSpec)
        state.set("scale", 2)
        assertEquals(2f, state.float("scale"))
        state.set("rotation", 45.5)
        assertEquals(45.5f, state.float("rotation"))
    }

    @Test
    fun set_rejectsUnknownKeysWithHelpfulMessage() {
        val state = ShaderState(MeshGradientSpec)
        val e = assertThrows(IllegalArgumentException::class.java) { state.set("distorsion", 0.5f) }
        assertTrue(e.message!!, e.message!!.contains("distortion"))
    }

    @Test
    fun set_rejectsWrongTypes() {
        val state = ShaderState(MeshGradientSpec)
        assertThrows(IllegalArgumentException::class.java) { state.set("scale", "big") }
        assertThrows(IllegalArgumentException::class.java) { state.set("fit", "stretch") }
        assertThrows(IllegalArgumentException::class.java) { state.set("colors", 0xFFFFFFFF) }
        assertThrows(IllegalArgumentException::class.java) { state.set("colors", emptyList<ArgbColor>()) }
    }

    @Test
    fun set_rejectsTooManyColors() {
        val spec = ShaderCatalog.all.first { s -> s.params.any { it is ColorsParam } }
        val p = spec.params.filterIsInstance<ColorsParam>().first()
        val state = ShaderState(spec)
        assertThrows(IllegalArgumentException::class.java) {
            state.set(p.key, List(p.maxCount + 1) { ArgbColor(0xFF000000) })
        }
    }

    @Test
    fun set_acceptsComposeColors() {
        val spec = ShaderCatalog.all.first { s -> s.params.any { it is ColorsParam } }
        val key = spec.params.first { it is ColorsParam }.key
        val state = ShaderState(spec)
        state.set(key, listOf(Color.Red, Color(0x80123456)))
        assertEquals(listOf(ArgbColor(0xFFFF0000), ArgbColor(0x80123456)), state.colors(key))
    }

    @Test
    fun everyPresetValue_passesValidation() {
        // Whatever Paper's presets contain must also be settable by hand.
        for (spec in ShaderCatalog.all) {
            val state = ShaderState(spec)
            for (preset in spec.presets) for ((key, value) in preset.values) state.set(key, value)
        }
    }

    @Test
    fun specDefaults_areSettableEvenWhenNotParams() {
        val spec = ShaderCatalog.all.first { it.defaults.isNotEmpty() }
        val (key, value) = spec.defaults.entries.first()
        val state = ShaderState(spec)
        assertEquals(value, state[key])
        if (value is Boolean) {
            state.set(key, !value)
            assertEquals(!value, state[key])
        }
    }

    @Test
    fun snapshot_isACopy() {
        val state = ShaderState(MeshGradientSpec)
        val snap = state.snapshot()
        state.set("scale", 3.5f)
        assertNotEquals(3.5f, snap["scale"])
        assertEquals(3.5f, state.float("scale"))
    }

    @Test
    fun typedGetters_returnFallbacksForMissingOrMistypedKeys() {
        val state = ShaderState(MeshGradientSpec)
        assertEquals(7f, state.float("nope", 7f))
        assertEquals(false, state.bool("nope"))
        assertNull(state.option("nope"))
        assertNull(state.color("nope"))
        assertTrue(state.colors("nope").isEmpty())
        assertEquals(9f, state.float("fit", 9f)) // fit is a String option, not a Float
    }
}
