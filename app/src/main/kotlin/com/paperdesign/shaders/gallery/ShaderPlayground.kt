package com.paperdesign.shaders.gallery

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.paperdesign.shaders.PaperShader
import com.paperdesign.shaders.ShaderClock
import com.paperdesign.shaders.ShaderSpec
import com.paperdesign.shaders.ShaderState
import com.paperdesign.shaders.ShaderInput
import com.paperdesign.shaders.TiltSensorEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Full-screen shader with a controls sheet for presets and every parameter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaderPlayground(spec: ShaderSpec, onBack: () -> Unit) {
    val state = remember(spec) { ShaderState(spec) }
    val clock = remember(spec) { ShaderClock(state.float("frame")) }
    val input = remember(spec) { ShaderInput() }
    var gestures by rememberSaveable(spec.id) { mutableStateOf(false) }
    var tiltParallax by rememberSaveable(spec.id) { mutableStateOf(false) }
    TiltSensorEffect(input, enabled = spec.usesTilt || tiltParallax)
    input.parallax = if (tiltParallax) TILT_PARALLAX else 0f
    var playing by rememberSaveable(spec.id) { mutableStateOf(true) }
    var showControls by rememberSaveable(spec.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                state.image = withContext(Dispatchers.IO) { decodeScaled(context, uri) }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        PaperShader(
            state,
            Modifier
                .fillMaxSize()
                .shaderPointer(input)
                .shaderGestures(state, gestures)
                .testTag("shader"),
            playing = playing,
            clock = clock,
            input = input,
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            spec.interactionHint?.let { hint ->
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)) {
                    Text(hint, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = gestures,
                    onClick = { gestures = !gestures },
                    label = { Text("Pan · zoom · rotate") },
                    colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)),
                    modifier = Modifier.testTag("toggle-gestures"),
                )
                if (!spec.usesTilt) {
                    FilterChip(
                        selected = tiltParallax,
                        onClick = { tiltParallax = !tiltParallax },
                        label = { Text("Tilt parallax") },
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)),
                        modifier = Modifier.testTag("toggle-tilt"),
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(spec.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (spec.animated && playing) "${clock.fps.roundToInt()} fps · ${state.presetName}" else state.presetName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("fps"),
                    )
                }
            }
            Box(Modifier.weight(1f))
            if (spec.animated) {
                FilledTonalIconButton(onClick = { playing = !playing }) {
                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (playing) "Pause" else "Play")
                }
            }
            FilledTonalIconButton(onClick = { showControls = true }, modifier = Modifier.testTag("open-controls")) {
                Icon(Icons.Filled.Tune, contentDescription = "Controls")
            }
        }
    }

    if (showControls) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showControls = false },
            sheetState = sheet,
            scrimColor = Color.Transparent,
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
        ) {
            ShaderControls(
                state = state,
                onPickImage = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onResetTime = { clock.timeMs = state.float("frame") },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

/** How far (in graphic-box units) a full tilt moves the pattern in parallax mode. */
private const val TILT_PARALLAX = 0.6f

/** Decodes a picked image, capped at 2048 px on the long side to keep GPU memory sane. */
private fun decodeScaled(context: android.content.Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val long = maxOf(info.size.width, info.size.height)
            if (long > 2048) {
                val k = 2048f / long
                decoder.setTargetSize((info.size.width * k).toInt(), (info.size.height * k).toInt())
            }
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()
