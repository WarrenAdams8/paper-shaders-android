package com.paperdesign.warp.demo

import java.util.Locale
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperdesign.warp.Warp
import com.paperdesign.warp.WarpParams
import com.paperdesign.warp.WarpPresets
import com.paperdesign.warp.WarpShape
import kotlin.math.roundToInt

/**
 * Interactive playground demonstration for the Paper Design Warp Shader.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarpPlayground(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var params by remember { mutableStateOf(WarpPresets.Default) }
    var selectedPresetName by remember { mutableStateOf("Default") }
    var isPlaying by remember { mutableStateOf(true) }
    var isControlsExpanded by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCodeDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Fullscreen Shader canvas
        Warp(
            modifier = Modifier.fillMaxSize(),
            params = params,
            isPlaying = isPlaying
        )

        // Floating Header Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.65f),
                contentColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Paper Design Warp",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "AGSL GPU Shader • Android 13+",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isPlaying = !isPlaying }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                val current = WarpPresets.AllPresets.firstOrNull { it.first == selectedPresetName }?.second ?: WarpPresets.Default
                                params = current
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Preset",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { showCodeDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Export Code",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset chips scroll row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WarpPresets.AllPresets.forEach { (name, presetParams) ->
                            val isSelected = selectedPresetName == name
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPresetName = name
                                    params = presetParams
                                },
                                label = { Text(name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Bottom Control Panel Overlay
        Card(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161618).copy(alpha = 0.94f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(250))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Drag handle & toggle header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isControlsExpanded = !isControlsExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shader Parameters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    IconButton(onClick = { isControlsExpanded = !isControlsExpanded }) {
                        Icon(
                            imageVector = if (isControlsExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isControlsExpanded) "Collapse" else "Expand",
                            tint = Color.White
                        )
                    }
                }

                AnimatedVisibility(visible = isControlsExpanded) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Pattern", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Distortion", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Blend & Colors", fontSize = 12.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (selectedTab) {
                                0 -> PatternTab(
                                    params = params,
                                    onParamsChange = { params = it }
                                )
                                1 -> DistortionTab(
                                    params = params,
                                    onParamsChange = { params = it }
                                )
                                2 -> BlendTab(
                                    params = params,
                                    onParamsChange = { params = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Kotlin Code Dialog
    if (showCodeDialog) {
        val snippet = generateKotlinSnippet(params)
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = { Text("Kotlin Snippet") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = snippet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Warp Shader Code", snippet))
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showCodeDialog = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun PatternTab(
    params: WarpParams,
    onParamsChange: (WarpParams) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Base Shape Pattern", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WarpShape.values().forEach { shape ->
                FilterChip(
                    selected = params.shape == shape,
                    onClick = { onParamsChange(params.copy(shape = shape)) },
                    label = { Text(shape.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        labelColor = Color.White
                    )
                )
            }
        }

        LabeledSlider(
            label = "Shape Scale",
            value = params.shapeScale,
            valueRange = 0f..1f,
            onValueChange = { onParamsChange(params.copy(shapeScale = it)) }
        )

        LabeledSlider(
            label = "Zoom Scale",
            value = params.scale,
            valueRange = 0.2f..3.0f,
            onValueChange = { onParamsChange(params.copy(scale = it)) }
        )

        LabeledSlider(
            label = "Rotation",
            value = params.rotation,
            valueRange = 0f..360f,
            formatValue = { "${it.roundToInt()}°" },
            onValueChange = { onParamsChange(params.copy(rotation = it)) }
        )
    }
}

@Composable
private fun DistortionTab(
    params: WarpParams,
    onParamsChange: (WarpParams) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledSlider(
            label = "Noise Distortion",
            value = params.distortion,
            valueRange = 0f..1f,
            onValueChange = { onParamsChange(params.copy(distortion = it)) }
        )

        LabeledSlider(
            label = "Swirl Strength",
            value = params.swirl,
            valueRange = 0f..1f,
            onValueChange = { onParamsChange(params.copy(swirl = it)) }
        )

        LabeledSlider(
            label = "Swirl Iterations",
            value = params.swirlIterations.toFloat(),
            valueRange = 2f..20f,
            steps = 17,
            formatValue = { it.roundToInt().toString() },
            onValueChange = { onParamsChange(params.copy(swirlIterations = it.roundToInt())) }
        )
    }
}

@Composable
private fun BlendTab(
    params: WarpParams,
    onParamsChange: (WarpParams) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledSlider(
            label = "Color Softness",
            value = params.softness,
            valueRange = 0f..2f,
            onValueChange = { onParamsChange(params.copy(softness = it)) }
        )

        LabeledSlider(
            label = "Proportion / Balance",
            value = params.proportion,
            valueRange = 0f..1f,
            onValueChange = { onParamsChange(params.copy(proportion = it)) }
        )

        LabeledSlider(
            label = "Speed",
            value = params.speed,
            valueRange = 0f..5f,
            formatValue = { String.format(Locale.US, "%.1fx", it) },
            onValueChange = { onParamsChange(params.copy(speed = it)) }
        )

        Text("Active Colors (${params.colors.size})", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            params.colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    formatValue: (Float) -> String = { String.format(Locale.US, "%.2f", it) },
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            Text(formatValue(value), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

private fun generateKotlinSnippet(params: WarpParams): String {
    val colorsCode = params.colors.joinToString(separator = ",\n        ") { color ->
        val hex = Integer.toHexString(color.toArgb()).uppercase().padStart(8, '0')
        "Color(0x$hex)"
    }

    return """
Warp(
    modifier = Modifier.fillMaxSize(),
    params = WarpParams(
        colors = listOf(
        $colorsCode
        ),
        proportion = ${params.proportion}f,
        softness = ${params.softness}f,
        distortion = ${params.distortion}f,
        swirl = ${params.swirl}f,
        swirlIterations = ${params.swirlIterations},
        shape = WarpShape.${params.shape.name},
        shapeScale = ${params.shapeScale}f,
        speed = ${params.speed}f,
        scale = ${params.scale}f,
        rotation = ${params.rotation}f,
        offsetX = ${params.offsetX}f,
        offsetY = ${params.offsetY}f
    )
)
""".trimIndent()
}

@Preview(showBackground = true)
@Composable
fun WarpPlaygroundPreview() {
    WarpPlayground()
}
