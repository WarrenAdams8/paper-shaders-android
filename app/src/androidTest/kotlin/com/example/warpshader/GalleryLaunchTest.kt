package com.example.warpshader

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Smoke test for the sample app: the gallery launches and a shader opens full screen. */
class GalleryLaunchTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun gallery_opensAShader() {
        // Animated shaders never let Compose go idle; drive the clock by hand instead.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Paper Shaders").assertExists()
        rule.onNodeWithText("Ripple Pond").performClick()
        rule.mainClock.advanceTimeBy(1_000)
        // The playground header repeats the shader title; the gallery title is gone.
        assertTrue(rule.onAllNodesWithText("Ripple Pond").fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("Paper Shaders").fetchSemanticsNodes().isEmpty())
    }
}
