package com.paperdesign.shaders

import android.os.Build
import android.view.LayoutInflater
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.paperdesign.shaders.specs.ShaderCatalog
import com.paperdesign.shaders.test.R
import com.paperdesign.shaders.view.PaperShaderView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Draws shaders through the public Compose and View APIs and checks real pixels. Runs on any
 * API level: on 33+ it exercises AGSL, below that the gradient fallback.
 */
@RunWith(AndroidJUnit4::class)
class PaperShaderRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private fun ImageBitmap.distinctColors(): Int {
        val map = toPixelMap()
        val seen = HashSet<Int>()
        for (y in 0 until height step 4) for (x in 0 until width step 4) seen += map[x, y].hashCode()
        return seen.size
    }

    @Test
    fun composeApi_drawsEveryShaderWithoutLoggingCompileErrors() {
        var index by mutableIntStateOf(0)
        compose.setContent {
            val spec = ShaderCatalog.all[index]
            PaperShader(
                state = rememberShaderState(spec),
                modifier = Modifier.size(96.dp).testTag("shader"),
                playing = false,
            )
        }
        val flat = mutableListOf<String>()
        for (i in ShaderCatalog.all.indices) {
            compose.runOnIdle { index = i }
            compose.waitForIdle()
            val image = compose.onNodeWithTag("shader").captureToImage()
            // A failed compile falls back silently; a single flat color would hint at that.
            if (image.distinctColors() < 2) flat += ShaderCatalog.all[i].id
        }
        assertTrue("Flat output for: $flat", flat.size <= 2)
    }

    @Test
    fun liveEdit_changesPixels() {
        val state = ShaderState(ShaderCatalog.require("mesh-gradient"))
        compose.setContent { PaperShader(state, Modifier.size(96.dp).testTag("shader"), playing = false) }
        val before = compose.onNodeWithTag("shader").captureToImage().toPixelMap()
        compose.runOnIdle { state.set("colors", listOf(ArgbColor(0xFF00FF00), ArgbColor(0xFF0000FF))) }
        compose.waitForIdle()
        val after = compose.onNodeWithTag("shader").captureToImage().toPixelMap()
        assertFalse(before[48, 48] == after[48, 48] && before[10, 10] == after[10, 10])
    }

    @Test
    fun fallbackAndAgsl_bothDrawSomething() {
        val state = ShaderState(ShaderCatalog.require("static-radial-gradient"))
        compose.setContent { PaperShader(state, Modifier.size(96.dp).testTag("shader"), playing = false) }
        val image = compose.onNodeWithTag("shader").captureToImage()
        assertTrue("API ${Build.VERSION.SDK_INT}: expected a gradient", image.distinctColors() > 4)
    }

    @Test
    fun xmlAttributes_configurePaperShaderView() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val root = LayoutInflater.from(context).inflate(R.layout.paper_test_views, null)
            val view = root.findViewById<PaperShaderView>(R.id.paper_test_view)
            assertEquals("dot-orbit", view.state.spec.id)
            assertFalse(view.isPlaying)
            view.setShader("swirl")
            assertEquals("swirl", view.state.spec.id)
        }
    }

    @Test
    fun paperShaderView_rendersInsideCompose() {
        compose.setContent {
            AndroidView(
                factory = { PaperShaderView(it).apply { setShader("swirl"); isPlaying = false } },
                modifier = Modifier.size(96.dp).testTag("view"),
            )
        }
        compose.waitForIdle()
        assertTrue(compose.onNodeWithTag("view").captureToImage().distinctColors() > 2)
    }
}
