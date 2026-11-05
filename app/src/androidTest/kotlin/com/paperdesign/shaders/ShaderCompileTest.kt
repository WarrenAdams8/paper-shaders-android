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

    /**
     * Saves each shader's default preset as a PNG (app external files dir, `renders/`) for a
     * visual check on devices where screenshots are unavailable (e.g. ATD emulator images).
     * Run with `-Pandroid.testInstrumentationRunnerArguments.saveRenders=true`.
     */
    @Test
    fun saveDefaultPresetRenders() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("saveRenders") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = java.io.File(context.getExternalFilesDir(null), "renders").apply { mkdirs() }
        val w = 360
        val h = 780
        for (spec in ShaderCatalog.all) {
            val program = ShaderProgram(spec, ShaderAssets.body(spec))
            val state = ShaderState(spec)
            val sample = ShaderAssets.sample(spec.sampleImage)
            val image = spec.preprocess?.let { ShaderAssets.process(sample, it) } ?: sample
            program.update(state, w.toFloat(), h.toFloat(), 1f, 3000f, image)
            val picture = Picture()
            picture.beginRecording(w, h).drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = program.shader })
            picture.endRecording()
            // Null when the device has no GPU rendering (e.g. ATD images).
            val hw: Bitmap? = Bitmap.createBitmap(picture)
            assumeTrue("No hardware rendering on this device", hw != null)
            val bmp = hw!!.copy(Bitmap.Config.ARGB_8888, false)
            java.io.File(dir, "${spec.id}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
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
                    val sample = ShaderAssets.sample(spec.sampleImage)
                    val image = spec.preprocess?.let { ShaderAssets.process(sample, it) } ?: sample
                    program.update(state, 64f, 96f, 2f, 1000f, image)
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
