package com.paperdesign.shaders.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** Camera-settings style palette for the controls sheet (always dark, whatever the theme). */
object ControlColors {
    val Sheet = Color(0xFF131416)
    val Handle = Color(0xFF5F6368)
    val Label = Color(0xFFE3E3E3)
    val Muted = Color(0xFF9AA0A6)
    val Value = Color(0xFFA8C7FA)
    val Chip = Color(0xFF2E3034)
    val ChipIcon = Color(0xFFE3E3E3)
    val Selected = Color(0xFFA8C7FA)
    val SelectedIcon = Color(0xFF062E6F)
    val Track = Color(0xFF3C4043)
}

/**
 * Every control for one shader, styled like a camera settings sheet: a bold title with a "…"
 * menu, then rows of label + current value on the left and round toggles, slim sliders or
 * swatches on the right.
 */
@Composable
fun ShaderControls(
    state: ShaderState,
    onPickImage: () -> Unit,
    onResetTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingColor by remember { mutableStateOf<ColorTarget?>(null) }
    var showSizing by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val spec = state.spec

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .testTag("controls"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                spec.title,
                color = ControlColors.Label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Box {
                RoundButton(Icons.Filled.MoreHoriz, "More", selected = false, onClick = { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Reset to ${state.presetName}") },
                        leadingIcon = { Icon(Icons.Filled.RestartAlt, null) },
                        onClick = {
                            spec.presets.firstOrNull { it.name == state.presetName }?.let(state::applyPreset)
                            onResetTime()
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (showSizing) "Hide sizing & motion" else "Sizing & motion") },
                        leadingIcon = { Icon(Icons.Filled.Tune, null) },
                        onClick = { showSizing = !showSizing; menuOpen = false },
                    )
                }
            }
        }
        Text(
            spec.description,
            color = ControlColors.Muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        // Presets: one numbered round button each; the name shows as the row's value.
        ControlRow("Preset", state.presetName) {
            spec.presets.forEachIndexed { i, preset ->
                RoundTextButton(
                    text = "${i + 1}",
                    description = preset.name,
                    selected = state.presetName == preset.name,
                    onClick = { state.applyPreset(preset); onResetTime() },
                )
            }
        }
        if (spec.usesImage) {
            ControlRow("Image", if (state.image == null) "Sample" else "Your photo") {
                RoundButton(Icons.Filled.Image, "Sample image", selected = state.image == null, onClick = { state.image = null })
                RoundButton(Icons.Filled.Photo, "Pick a photo", selected = state.image != null, onClick = onPickImage)
            }
        }
        spec.params.forEach { p -> ParamRow(p, state, onEditColor = { editingColor = it }) }
        if (showSizing) {
            Text(
                "Sizing & motion",
                color = ControlColors.Label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            CommonParams.forSpec(spec).forEach { p -> ParamRow(p, state, onEditColor = {}) }
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

/** Label with the current value under it (left), controls (right). */
@Composable
private fun ControlRow(label: String, value: String, controls: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, color = ControlColors.Label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = ControlColors.Value, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier
                .widthIn(max = 260.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { controls() }
    }
}

/** Label/value on the left, a slim slider filling the right half. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderRow(p: FloatParam, state: ShaderState) {
    val value = state.float(p.key)
    // Presets sometimes go past the documented range (e.g. Warp "Cauldron Pot" softness 1.5).
    val lo = minOf(p.min, value)
    val hi = maxOf(p.max, value)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(0.42f).padding(end = 12.dp)) {
            Text(p.label, color = ControlColors.Label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatValue(value, p.step), color = ControlColors.Value, fontSize = 14.sp)
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
            thumb = {
                Box(
                    Modifier
                        .size(20.dp)
                        .background(ControlColors.Selected, CircleShape),
                )
            },
            track = { sliderState -> SlimTrack(sliderState) },
            modifier = Modifier
                .weight(0.58f)
                .height(40.dp)
                .testTag("slider-${p.key}"),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlimTrack(sliderState: SliderState) {
    val range = sliderState.valueRange
    val fraction = ((sliderState.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(4.dp),
    ) {
        val y = size.height / 2
        val stroke = size.height
        drawLine(ControlColors.Track, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(ControlColors.Selected, Offset(0f, y), Offset(size.width * fraction, y), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun ParamRow(p: ParamDef, state: ShaderState, onEditColor: (ColorTarget) -> Unit) {
    when (p) {
        is FloatParam -> SliderRow(p, state)
        is BoolParam -> {
            val on = state.bool(p.key)
            ControlRow(p.label, if (on) "On" else "Off") {
                RoundButton(Icons.Filled.Close, "${p.label} off", selected = !on, onClick = { state.set(p.key, false) })
                RoundButton(Icons.Filled.Check, "${p.label} on", selected = on, onClick = { state.set(p.key, true) })
            }
        }
        is EnumParam -> {
            val current = state.option(p.key) ?: p.options.first()
            ControlRow(p.label, prettyOption(current)) {
                p.options.forEach { option ->
                    val icon = optionIcon(option)
                    if (icon != null) {
                        RoundButton(icon, prettyOption(option), selected = option == current, onClick = { state.set(p.key, option) })
                    } else {
                        RoundTextButton(abbreviate(option), prettyOption(option), selected = option == current, onClick = { state.set(p.key, option) })
                    }
                }
            }
        }
        is ColorParam -> {
            val c = state.color(p.key) ?: ArgbColor(0xFF000000)
            ControlRow(p.label, hex(c)) {
                Swatch(c, p.label) { onEditColor(ColorTarget(c) { state.set(p.key, it) }) }
            }
        }
        is ColorsParam -> {
            val list = state.colors(p.key)
            Column(Modifier.padding(vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.label, color = ControlColors.Label, fontSize = 16.sp)
                        Text("${list.size} of ${p.maxCount}", color = ControlColors.Value, fontSize = 14.sp)
                    }
                    RoundButton(Icons.Filled.Remove, "Remove color", selected = false, enabled = list.size > 1) {
                        state.set(p.key, list.dropLast(1))
                    }
                    Spacer(Modifier.width(10.dp))
                    RoundButton(Icons.Filled.Add, "Add color", selected = false, enabled = list.size < p.maxCount) {
                        state.set(p.key, list + (list.lastOrNull() ?: ArgbColor(0xFFFFFFFF)))
                    }
                }
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    list.forEachIndexed { i, c ->
                        Swatch(c, "${p.label} ${i + 1}") {
                            onEditColor(ColorTarget(c) { picked -> state.set(p.key, list.toMutableList().also { it[i] = picked }) })
                        }
                    }
                }
            }
        }
    }
}

/** 44dp round icon toggle: pale blue when selected, dark grey otherwise. */
@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) ControlColors.Selected else ControlColors.Chip)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val tint = when {
            !enabled -> ControlColors.Muted.copy(alpha = 0.4f)
            selected -> ControlColors.SelectedIcon
            else -> ControlColors.ChipIcon
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Round toggle showing a short text (for options without a fitting icon, and preset numbers). */
@Composable
private fun RoundTextButton(text: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) ControlColors.Selected else ControlColors.Chip)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) ControlColors.SelectedIcon else ControlColors.ChipIcon,
            fontSize = if (text.length > 2) 12.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun Swatch(c: ArgbColor, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ControlColors.Chip)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$description color" }
            .padding(5.dp)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .background(c.toColor(), CircleShape),
    )
}

/** Icons for the option names used across the Paper shaders (others fall back to text). */
private fun optionIcon(option: String): ImageVector? = when (option) {
    "circle", "dots", "classic" -> Icons.Filled.Circle
    "square" -> Icons.Filled.CropSquare
    "diamond" -> Icons.Filled.Diamond
    "triangle" -> Icons.Filled.ChangeHistory
    "none" -> Icons.Filled.Close
    "checks", "4x4" -> Icons.Filled.GridOn
    "stripes", "lines" -> Icons.Filled.ViewStream
    "wave", "waves" -> Icons.Filled.Waves
    "ripple" -> Icons.Filled.Adjust
    "random" -> Icons.Filled.Shuffle
    "pattern", "truchet" -> Icons.Filled.Texture
    "zigzag" -> Icons.Filled.ShowChart
    "hex" -> Icons.Filled.Hexagon
    "contain" -> Icons.Filled.FitScreen
    "cover" -> Icons.Filled.Crop
    "soft", "simplex" -> Icons.Filled.BlurOn
    "auto" -> Icons.Filled.AutoAwesome
    else -> null
}

private fun prettyOption(option: String): String =
    option.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }

private fun abbreviate(option: String): String = when {
    option.length <= 3 -> option
    else -> option.take(3).replaceFirstChar { it.uppercase() }
}

private fun hex(c: ArgbColor): String {
    val a = (c.argb shr 24) and 0xFF
    val rgb = String.format(Locale.US, "#%06X", c.argb and 0xFFFFFF)
    return if (a == 0xFFL) rgb else "$rgb · ${(a * 100 / 255)}%"
}

private fun formatValue(v: Float, step: Float): String =
    if (step >= 1f) v.roundToInt().toString() else String.format(Locale.US, "%.2f", v)
