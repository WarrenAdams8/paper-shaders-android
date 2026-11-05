package com.paperdesign.shaders.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.paperdesign.shaders.ArgbColor

/** Minimal HSV + alpha picker with a hex field. */
@Composable
fun ColorPickerDialog(initial: ArgbColor, onDismiss: () -> Unit, onPick: (ArgbColor) -> Unit) {
    val hsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.argb.toInt(), it) }
    }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    var a by remember { mutableFloatStateOf(((initial.argb shr 24) and 0xFF) / 255f) }
    val argb = (android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFFL)
    var hex by remember { mutableStateOf("%08X".format(initial.argb and 0xFFFFFFFFL)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(ArgbColor(argb)) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Color") },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(argb.toInt()), RoundedCornerShape(12.dp)),
                )
                Labeled("Hue") { Slider(h, { h = it; hex = "%08X".format(currentArgb(a, h, s, v)) }, valueRange = 0f..360f) }
                Labeled("Saturation") { Slider(s, { s = it; hex = "%08X".format(currentArgb(a, h, s, v)) }) }
                Labeled("Value") { Slider(v, { v = it; hex = "%08X".format(currentArgb(a, h, s, v)) }) }
                Labeled("Alpha") { Slider(a, { a = it; hex = "%08X".format(currentArgb(a, h, s, v)) }) }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { text ->
                        hex = text.uppercase().filter { it in "0123456789ABCDEF" }.take(8)
                        val parsed = when (hex.length) {
                            6 -> ("FF$hex").toLongOrNull(16)
                            8 -> hex.toLongOrNull(16)
                            else -> null
                        }
                        if (parsed != null) {
                            val out = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed.toInt(), out)
                            h = out[0]; s = out[1]; v = out[2]
                            a = ((parsed shr 24) and 0xFF) / 255f
                        }
                    },
                    label = { Text("AARRGGBB") },
                    singleLine = true,
                )
            }
        },
    )
}

private fun currentArgb(a: Float, h: Float, s: Float, v: Float): Long =
    android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFFL

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(84.dp), style = MaterialTheme.typography.labelMedium)
        Box(Modifier.weight(1f)) { content() }
    }
}
