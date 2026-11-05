package com.paperdesign.shaders.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.paperdesign.shaders.ArgbColor
import com.paperdesign.shaders.BoolParam
import com.paperdesign.shaders.ColorParam
import com.paperdesign.shaders.ColorsParam
import com.paperdesign.shaders.CommonParams
import com.paperdesign.shaders.EnumParam
import com.paperdesign.shaders.FloatParam
import com.paperdesign.shaders.ParamDef
import com.paperdesign.shaders.ShaderState
import com.paperdesign.shaders.toColor
import java.util.Locale
import kotlin.math.roundToInt

/** Every control for one shader: presets, its own params, then the shared sizing params. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShaderControls(
    state: ShaderState,
    onPickImage: () -> Unit,
    onResetTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingColor by remember { mutableStateOf<ColorTarget?>(null) }
    var showSizing by remember { mutableStateOf(false) }
    val spec = state.spec

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .testTag("controls"),
    ) {
        Text(spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        SectionTitle("Presets")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spec.presets.forEach { preset ->
                FilterChip(
                    selected = state.presetName == preset.name,
                    onClick = { state.applyPreset(preset); onResetTime() },
                    label = { Text(preset.name) },
                )
            }
        }
        if (spec.usesImage) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onPickImage, label = { Text("Pick image") })
                AssistChip(onClick = { state.image = null }, label = { Text("Sample image") })
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        spec.params.forEach { p ->
            ParamControl(p, state, onEditColor = { editingColor = it })
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showSizing = !showSizing }) {
            Text(if (showSizing) "Hide sizing & motion" else "Sizing & motion")
        }
        if (showSizing) {
            CommonParams.forSpec(spec).forEach { p -> ParamControl(p, state, onEditColor = {}) }
        }
    }

    editingColor?.let { target ->
        ColorPickerDialog(
            initial = target.current,
            onDismiss = { editingColor = null },
            onPick = { picked ->
                target.apply(picked)
                editingColor = null
            },
        )
    }
}

/** A color being edited and how to write it back. */
class ColorTarget(val current: ArgbColor, val apply: (ArgbColor) -> Unit)

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 6.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParamControl(p: ParamDef, state: ShaderState, onEditColor: (ColorTarget) -> Unit) {
    when (p) {
        is FloatParam -> FloatControl(p, state)
        is BoolParam -> Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.bool(p.key), onCheckedChange = { state.set(p.key, it) })
        }
        is EnumParam -> Column(Modifier.padding(vertical = 4.dp)) {
            Text(p.label, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val current = state.option(p.key)
                p.options.forEach { option ->
                    FilterChip(selected = current == option, onClick = { state.set(p.key, option) }, label = { Text(option) })
                }
            }
        }
        is ColorParam -> Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            val c = state.color(p.key) ?: ArgbColor(0xFF000000)
            Swatch(c) { onEditColor(ColorTarget(c) { state.set(p.key, it) }) }
        }
        is ColorsParam -> Column(Modifier.padding(vertical = 6.dp)) {
            val list = state.colors(p.key)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${p.label} (${list.size}/${p.maxCount})", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(enabled = list.size > 1, onClick = { state.set(p.key, list.dropLast(1)) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Remove color")
                }
                IconButton(enabled = list.size < p.maxCount, onClick = { state.set(p.key, list + (list.lastOrNull() ?: ArgbColor(0xFFFFFFFF))) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add color")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEachIndexed { i, c ->
                    Swatch(c) {
                        onEditColor(ColorTarget(c) { picked -> state.set(p.key, list.toMutableList().also { it[i] = picked }) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatControl(p: FloatParam, state: ShaderState) {
    val value = state.float(p.key)
    // Presets sometimes go past the documented range (e.g. Warp "Cauldron Pot" softness 1.5).
    val lo = minOf(p.min, value)
    val hi = maxOf(p.max, value)
    Column(Modifier.padding(top = 4.dp)) {
        Row {
            Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(formatValue(value, p.step), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
        }
        val steps = if (p.step > 0f) (((hi - lo) / p.step).roundToInt() - 1).coerceIn(0, 200) else 0
        Slider(
            value = value,
            onValueChange = { v ->
                val snapped = if (p.step > 0f) (Math.round(v / p.step) * p.step) else v
                state.set(p.key, snapped)
            },
            valueRange = lo..hi,
            steps = steps,
            modifier = Modifier.testTag("slider-${p.key}"),
        )
    }
}

private fun formatValue(v: Float, step: Float): String =
    if (step >= 1f) v.roundToInt().toString() else String.format(Locale.US, "%.2f", v)

@Composable
private fun Swatch(c: ArgbColor, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .padding(3.dp)
            .background(c.toColor(), CircleShape)
            .clickable(onClick = onClick),
    )
}
