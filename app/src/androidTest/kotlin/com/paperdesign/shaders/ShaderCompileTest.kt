package com.paperdesign.shaders

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Picture
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.paperdesign.shaders.specs.ShaderCatalog
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Compiles every AGSL shader and renders each preset once into a bitmap. */
@RunWith(AndroidJUnit4::class)
class ShaderCompileTest {

    @Before
    fun setUp() {
        assumeTrue("AGSL needs API 33+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ShaderAssets.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun allShadersCompileAndRenderEveryPreset() {
        val failures = mutableListOf<String>()
        for (spec in ShaderCatalog.all) {
            val program = runCatching { ShaderProgram(spec, ShaderAssets.body(spec)) }
                .getOrElse { failures += "${spec.id}: ${it.message}"; null } ?: continue
            for (preset in spec.presets) {
                runCatching {
                    val state = ShaderState(spec, preset)
                    program.update(state, 64f, 96f, 2f, 1000f, ShaderAssets.sampleImage)
                    // RuntimeShader needs hardware rendering; a Picture-backed bitmap renders on the GPU.
                    val picture = Picture()
                    picture.beginRecording(64, 96).drawRect(0f, 0f, 64f, 96f, Paint().apply { shader = program.shader })
                    picture.endRecording()
                    Bitmap.createBitmap(picture)
                }.onFailure { failures += "${spec.id}/${preset.name}: ${it.message}" }
            }
        }
        failures.forEach { android.util.Log.e("ShaderCompileTest", it) }
        assertTrue("${failures.size} shader failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
