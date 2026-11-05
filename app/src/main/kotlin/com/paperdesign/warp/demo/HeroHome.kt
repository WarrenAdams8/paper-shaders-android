package com.paperdesign.warp.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperdesign.shaders.PaperShader
import com.paperdesign.shaders.Preset
import com.paperdesign.shaders.ShaderState
import com.paperdesign.shaders.colors
import com.paperdesign.shaders.specs.WarpSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Example hero home screen for a fictional fitness app ("Stride"), showing the Warp shader as an
 * animated header that fades into a dark page.
 *
 * Performance: one [ShaderState] lives for the whole screen. Switching the color theme calls
 * [ShaderState.applyPreset], which only re-uploads uniforms on the next draw; the AGSL program is
 * compiled once, and per frame only `u_time` changes (see [PaperShader]).
 */
@Composable
fun HeroHomeScreen(modifier: Modifier = Modifier) {
    var themeIndex by rememberSaveable { mutableIntStateOf(0) }
    val theme = HeroThemes[themeIndex]
    val warp = remember { ShaderState(WarpSpec, HeroThemes[0].preset) }
    // Uniform-only update: same program, new values.
    if (warp.presetName != theme.preset.name) warp.applyPreset(theme.preset)

    Box(
        modifier
            .fillMaxSize()
            .background(PageBlack)
            .testTag("hero-home"),
    ) {
        HeroBackground(warp, Modifier.fillMaxWidth().fillMaxHeight(0.56f))

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(onCycleTheme = { themeIndex = (themeIndex + 1) % HeroThemes.size }, themeName = theme.name)
            Spacer(Modifier.height(28.dp))
            TodayLabel()
            Spacer(Modifier.weight(0.6f))
            Headline(theme)
            Spacer(Modifier.height(28.dp))
            PrimaryButton(theme)
            Text(
                theme.secondary,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) {}
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(Modifier.weight(0.4f))
            StatRow(theme)
            Spacer(Modifier.height(24.dp))
            BottomNav(theme, Modifier.navigationBarsPadding().padding(bottom = 12.dp))
        }
    }
}

/** The Warp field with a vertical fade to the page color drawn on top. */
@Composable
private fun HeroBackground(warp: ShaderState, modifier: Modifier) {
    Box(modifier) {
        PaperShader(warp, Modifier.fillMaxSize())
        // Static overlay: drawn from a remembered brush, never recomposed per frame.
        val fade = remember {
            Brush.verticalGradient(
                0.00f to Color.Black.copy(alpha = 0.10f),
                0.45f to Color.Transparent,
                0.72f to PageBlack.copy(alpha = 0.55f),
                1.00f to PageBlack,
            )
        }
        Box(Modifier.fillMaxSize().background(fade))
    }
}

@Composable
private fun TopBar(onCycleTheme: () -> Unit, themeName: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "stride",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.weight(1f))
        GlassIconButton(Icons.Filled.Palette, "Color theme: $themeName", onCycleTheme, Modifier.testTag("hero-theme"))
        Spacer(Modifier.width(10.dp))
        GlassIconButton(Icons.Outlined.Notifications, "Notifications", {})
        Spacer(Modifier.width(10.dp))
        GlassIconButton(Icons.Outlined.Person, "Profile", {})
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TodayLabel() {
    val date = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TickArc(leftSide = true)
        Column(Modifier.padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TODAY", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, style = LabelShadow)
            Text(date, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, style = LabelShadow)
        }
        TickArc(leftSide = false)
    }
}

/** A short arc of tick marks, curving away from the label like a dial. */
@Composable
private fun TickArc(leftSide: Boolean) {
    Canvas(Modifier.size(width = 64.dp, height = 40.dp)) {
        val ticks = 9
        val radius = size.width * 2.2f
        // Arc centre sits far below the label so the ticks bow upward.
        val center = Offset(if (leftSide) size.width * 1.15f else -size.width * 0.15f, size.height / 2 + radius)
        val sweep = 0.38f
        val mid = if (leftSide) -PI.toFloat() / 2 - sweep / 2 - 0.05f else -PI.toFloat() / 2 + sweep / 2 + 0.05f
        for (i in 0 until ticks) {
            val f = i / (ticks - 1f)
            val a = mid - sweep / 2 + sweep * f
            // Fade toward the outer end.
            val outer = if (leftSide) 1f - f else f
            val alpha = 0.85f - 0.65f * outer
            val len = if (i % 4 == 0) 10.dp.toPx() else 6.dp.toPx()
            val dir = Offset(cos(a), sin(a))
            val p0 = center + dir * radius
            val p1 = center + dir * (radius - len)
            drawLine(Color.White.copy(alpha = alpha), p0, p1, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun Headline(theme: HeroTheme) {
    val gloss = remember {
        Brush.verticalGradient(listOf(Color.White, Color(0xFFE9E9EE), Color(0xFF9A9AA3)))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                theme.bigValue,
                style = TextStyle(brush = gloss, fontSize = 112.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-4).sp, lineHeight = 112.sp),
            )
            if (theme.bigUnit.isNotEmpty()) {
                Text(
                    theme.bigUnit,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 22.dp),
                )
            }
        }
        Text(
            theme.title,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun PrimaryButton(theme: HeroTheme) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(CircleShape)
            .background(theme.accent)
            .clickable(role = Role.Button) {}
            .testTag("hero-cta"),
        contentAlignment = Alignment.Center,
    ) {
        Text(theme.cta, color = theme.onAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatRow(theme: HeroTheme) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(theme.statValue, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Text(
                    theme.statUnit,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 9.dp),
                )
            }
            Text(theme.statLabel, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }
        Row(
            Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(role = Role.Button) {}
                .padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add meal", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BottomNav(theme: HeroTheme, modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val items = listOf(
        Icons.Filled.Home to "Home",
        Icons.Outlined.BarChart to "Progress",
        Icons.Outlined.Person to "Profile",
    )
    Row(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val on = i == selected
            Box(
                Modifier
                    .size(width = 64.dp, height = 48.dp)
                    .clip(CircleShape)
                    .background(if (on) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable(role = Role.Tab) { selected = i },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = if (on) theme.accent else Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

/** Placeholder copy and colors for one look of the screen. */
@Immutable
data class HeroTheme(
    val name: String,
    val preset: Preset,
    val accent: Color,
    val onAccent: Color,
    val bigValue: String,
    val bigUnit: String,
    val title: String,
    val cta: String,
    val secondary: String,
    val statValue: String,
    val statUnit: String,
    val statLabel: String,
)

private val PageBlack = Color(0xFF09090B)

/** Keeps small white labels legible over the brightest parts of the gradient. */
private val LabelShadow = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 10f))

private fun warpPreset(name: String, vararg argb: Long, rotation: Float) = Preset(
    name,
    mapOf(
        "fit" to "none",
        "scale" to 1.4f,
        "rotation" to rotation,
        "speed" to 1.2f,
        "colors" to colors(*argb),
        "proportion" to 0.42f,
        "softness" to 1.0f,
        "distortion" to 0.28f,
        "swirl" to 0.7f,
        "swirlIterations" to 8.0f,
        "shapeScale" to 0.18f,
        "shape" to "checks",
    ),
)

val HeroThemes: List<HeroTheme> = listOf(
    HeroTheme(
        name = "Coral",
        preset = warpPreset("Hero Coral", 0xff1a0606L, 0xffff3b30L, 0xffff6f4fL, 0xff7a0f1aL, 0xffffa58aL, rotation = 20f),
        accent = Color(0xFFFF5A43),
        onAccent = Color.White,
        bigValue = "5",
        bigUnit = "moves",
        title = "Core & Glutes",
        cta = "Start session",
        secondary = "Preview moves",
        statValue = "640",
        statUnit = "KCAL",
        statLabel = "Energy balance",
    ),
    HeroTheme(
        name = "Neon",
        preset = warpPreset("Hero Neon", 0xff04140aL, 0xff39ff6aL, 0xffb6ff3bL, 0xff0b5a2aL, 0xff7dffb0L, rotation = 200f),
        accent = Color(0xFF5CFF7A),
        onAccent = Color(0xFF04140A),
        bigValue = "0:45",
        bigUnit = "rest",
        title = "Breathe, then go",
        cta = "See recap",
        secondary = "Skip rest",
        statValue = "312",
        statUnit = "KCAL",
        statLabel = "Burned so far",
    ),
)
