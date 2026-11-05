package com.paperdesign.shaders.gallery

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.paperdesign.shaders.ShaderInput
import com.paperdesign.shaders.ShaderState
import kotlin.math.min

/**
 * Feeds raw pointer events into [input] (for shaders that read `u_pointer` / `u_touches`).
 * Observes in the Initial pass and consumes nothing, so gesture handling still works.
 */
fun Modifier.shaderPointer(input: ShaderInput): Modifier = pointerInput(input) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull() ?: continue
            when (event.type) {
                PointerEventType.Press -> input.pointerDown(change.position.x, change.position.y)
                PointerEventType.Move -> if (change.pressed) input.pointerMove(change.position.x, change.position.y)
                PointerEventType.Release -> if (event.changes.none { it.pressed }) input.pointerUp()
                else -> Unit
            }
        }
    }
}

/**
 * Drag to pan, pinch to zoom, twist to rotate: maps gestures onto the shared sizing params
 * (offsetX/Y, scale, rotation) that every Paper shader has.
 */
fun Modifier.shaderGestures(state: ShaderState, enabled: Boolean): Modifier =
    if (!enabled) this else pointerInput(state) {
        detectTransformGestures { _, pan, zoom, rotationDeg ->
            val scale = state.float("scale", 1f)
            // Offsets are in units of the graphic box; the box is about the smaller side.
            val box = min(size.width, size.height).toFloat().coerceAtLeast(1f)
            val newScale = (scale * zoom).coerceIn(0.05f, 8f)
            state.set("scale", newScale)
            state.set("offsetX", (state.float("offsetX") + pan.x / box).coerceIn(-4f, 4f))
            state.set("offsetY", (state.float("offsetY") + pan.y / box).coerceIn(-4f, 4f))
            state.set("rotation", (state.float("rotation") - rotationDeg + 360f) % 360f)
        }
    }
