package com.paperdesign.shaders.gallery

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.DpSize
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
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

/**
 * Color roles for the controls, taken from the app's Material You scheme (wallpaper colors,
 * system light/dark), so the camera-settings layout follows the user's theme.
 */
object ControlColors {
    val Sheet: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val Handle: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Label: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Value: Color @Composable get() = MaterialTheme.colorScheme.primary
    val Chip: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest
    val ChipIcon: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val Selected: Color @Composable get() = MaterialTheme.colorScheme.primary
    val SelectedIcon: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val Track: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
}

/**
 * Every control for one shader, in a camera-settings style built from Material 3 Expressive
 * parts: a bold title with a "…" menu, label + current value rows, connected button groups
 * (ToggleButton) for options, expressive sliders for continuous values, and round swatches.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    run {
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
                    FilledTonalIconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreHoriz, contentDescription = "More")
                    }
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
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            Setting("Preset", state.presetName) {
                ConnectedGroup(
                    items = spec.presets.map { it.name },
                    selected = state.presetName,
                    scrollable = true,
                    onSelect = { name ->
                        spec.presets.firstOrNull { it.name == name }?.let(state::applyPreset)
                        onResetTime()
                    },
                    content = { name, _ -> Text(name, maxLines = 1) },
                )
            }
            if (spec.usesImage) {
                InlineSetting("Image", if (state.image == null) "Sample" else "Your photo") {
                    ConnectedGroup(
                        items = listOf("sample", "photo"),
                        selected = if (state.image == null) "sample" else "photo",
                        onSelect = { if (it == "sample") state.image = null else onPickImage() },
                        content = { item, _ ->
                            Icon(if (item == "sample") Icons.Filled.Image else Icons.Filled.AddPhotoAlternate, contentDescription = if (item == "sample") "Sample image" else "Pick a photo")
                        },
                    )
                }
            }
            spec.params.forEach { p -> ParamSetting(p, state, onEditColor = { editingColor = it }) }
            if (showSizing) {
                Text(
                    "Sizing & motion",
                    color = ControlColors.Label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
                CommonParams.forSpec(spec).forEach { p -> ParamSetting(p, state, onEditColor = {}) }
            }
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
private fun LabelValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = ControlColors.Label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = ControlColors.Value, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Label/value line with the control underneath, full width. */
@Composable
private fun Setting(label: String, value: String, control: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        LabelValue(label, value)
        Spacer(Modifier.height(10.dp))
        control()
    }
}

/** Label/value on the left, a compact control on the right. */
@Composable
private fun InlineSetting(label: String, value: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        LabelValue(label, value, Modifier.weight(1f).padding(end = 12.dp))
        control()
    }
}

/**
 * M3 Expressive connected button group: ToggleButtons sharing one rounded outline, the
 * selected one filled and morphing to a pill. Scrollable for long lists (e.g. preset names).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ConnectedGroup(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    scrollable: Boolean = false,
    fill: Boolean = false,
    content: @Composable (T, Boolean) -> Unit,
) {
    val rowModifier = when {
        scrollable -> Modifier.horizontalScroll(rememberScrollState())
        fill -> Modifier.fillMaxWidth()
        else -> Modifier
    }
    Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        items.forEachIndexed { i, item ->
            val checked = item == selected
            val shapes = when {
                items.size == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
                i == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                i == items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(item) },
                shapes = shapes,
                colors = ToggleButtonDefaults.colors(
                    containerColor = ControlColors.Chip,
                    contentColor = ControlColors.Label,
                    checkedContainerColor = ControlColors.Selected,
                    checkedContentColor = ControlColors.SelectedIcon,
                ),
                contentPadding = if (scrollable) ButtonDefaults.ContentPadding else PaddingValues(horizontal = 4.dp),
                modifier = (if (fill) Modifier.weight(1f) else Modifier)
                    .semantics { role = Role.RadioButton }
                    .testTag("option-$item"),
            ) { content(item, checked) }
        }
    }
}

/** Expressive slider: tall rounded track with a gap around a thin bar thumb. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SliderSetting(p: FloatParam, state: ShaderState) {
    val value = state.float(p.key)
    // Presets sometimes go past the documented range (e.g. Warp "Cauldron Pot" softness 1.5).
    val lo = minOf(p.min, value)
    val hi = maxOf(p.max, value)
    val steps = if (p.step > 0f) (((hi - lo) / p.step).roundToInt() - 1).coerceIn(0, 200) else 0
    val colors = SliderDefaults.colors(
        thumbColor = ControlColors.Selected,
        activeTrackColor = ControlColors.Selected,
        inactiveTrackColor = ControlColors.Track,
        activeTickColor = ControlColors.SelectedIcon,
        inactiveTickColor = ControlColors.Muted,
    )
    val interaction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        LabelValue(p.label, formatValue(value, p.step))
        val sliderSteps = if (steps > 24) 0 else steps
        val sliderState = remember(sliderSteps, lo, hi) { SliderState(value, sliderSteps, lo..hi) }
        sliderState.value = value
        Slider(
            state = sliderState,
            onValueChange = { v ->
                val snapped = if (p.step > 0f) (Math.round(v / p.step) * p.step) else v
                state.set(p.key, snapped)
            },
            colors = colors,
            interactionSource = interaction,
            thumb = { _ -> SliderDefaults.Thumb(interactionSource = interaction, colors = colors, thumbSize = DpSize(4.dp, 36.dp)) },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = colors,
                    modifier = Modifier.height(16.dp),
                    thumbTrackGapSize = 5.dp,
                    trackInsideCornerSize = 2.dp,
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("slider-${p.key}"),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ParamSetting(p: ParamDef, state: ShaderState, onEditColor: (ColorTarget) -> Unit) {
    when (p) {
        is FloatParam -> SliderSetting(p, state)
        is BoolParam -> {
            val on = state.bool(p.key)
            InlineSetting(p.label, if (on) "On" else "Off") {
                ConnectedGroup(
                    items = listOf(false, true),
                    selected = on,
                    onSelect = { state.set(p.key, it) },
                    content = { item, _ ->
                        Icon(if (item) Icons.Filled.Check else Icons.Filled.Close, contentDescription = "${p.label} ${if (item) "on" else "off"}")
                    },
                )
            }
        }
        is EnumParam -> {
            val current = state.option(p.key) ?: p.options.first()
            val allIcons = p.options.all { optionIcon(it) != null }
            Setting(p.label, prettyOption(current)) {
                ConnectedGroup(
                    items = p.options,
                    selected = current,
                    onSelect = { state.set(p.key, it) },
                    fill = true,
                    content = { option, _ ->
                        val icon = optionIcon(option)
                        if (icon != null && (allIcons || p.options.size > 4)) {
                            Icon(icon, contentDescription = prettyOption(option))
                        } else {
                            Text(if (p.options.size > 4) abbreviate(option) else prettyOption(option), maxLines = 1, fontSize = 13.sp)
                        }
                    },
                )
            }
        }
        is ColorParam -> {
            val c = state.color(p.key) ?: ArgbColor(0xFF000000)
            InlineSetting(p.label, hex(c)) {
                Swatch(c, p.label) { onEditColor(ColorTarget(c) { state.set(p.key, it) }) }
            }
        }
        is ColorsParam -> {
            val list = state.colors(p.key)
            Column(Modifier.padding(vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabelValue(p.label, "${list.size} of ${p.maxCount}", Modifier.weight(1f))
                    FilledTonalIconButton(enabled = list.size > 1, onClick = { state.set(p.key, list.dropLast(1)) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Remove color")
                    }
                    FilledTonalIconButton(enabled = list.size < p.maxCount, onClick = { state.set(p.key, list + (list.lastOrNull() ?: ArgbColor(0xFFFFFFFF))) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add color")
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
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
