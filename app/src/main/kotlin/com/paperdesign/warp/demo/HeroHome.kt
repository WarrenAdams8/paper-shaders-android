package com.paperdesign.warp.demo

import androidx.compose.foundation.Canvas
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.paperdesign.shaders.color
import com.paperdesign.shaders.specs.LiquidMetalSpec
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.foundation.border
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
        HeroBackground(warp, Modifier.fillMaxWidth().fillMaxHeight(0.62f))

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
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) {}
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(theme.secondary, color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(20.dp))
            }
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
                // The warp draws its own wavy edge into the dark; this only hides the box's bottom edge.
                0.00f to Color.Transparent,
                0.78f to Color.Transparent,
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
        GlassIconButton(Icons.Outlined.LocalFireDepartment, "Streak", {})
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
    Canvas(Modifier.size(width = 88.dp, height = 44.dp)) {
        val ticks = 26
        val radius = size.width * 2.2f
        // Arc centre sits far below the label so the ticks bow upward.
        val center = Offset(if (leftSide) size.width * 1.15f else -size.width * 0.15f, size.height / 2 + radius)
        val sweep = 0.42f
        val mid = if (leftSide) -PI.toFloat() / 2 - sweep / 2 - 0.05f else -PI.toFloat() / 2 + sweep / 2 + 0.05f
        for (i in 0 until ticks) {
            val f = i / (ticks - 1f)
            val a = mid - sweep / 2 + sweep * f
            // Fade toward the outer end.
            val outer = if (leftSide) 1f - f else f
            val alpha = 0.75f - 0.68f * outer
            val len = 12.dp.toPx() - 4.dp.toPx() * outer
            val dir = Offset(cos(a), sin(a))
            val p0 = center + dir * radius
            val p1 = center + dir * (radius - len)
            drawLine(Color.White.copy(alpha = alpha), p0, p1, strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun Headline(theme: HeroTheme) {
    // Chrome-like fill: bright top, a darker band, then a lighter lip at the bottom.
    val gloss = remember {
        Brush.verticalGradient(
            0.00f to Color.White,
            0.45f to Color(0xFFD9D9DF),
            0.62f to Color(0xFF8C8C96),
            1.00f to Color(0xFFC9C9D1),
        )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        theme.badge?.let { badge ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(badge.uppercase(), color = theme.accent, fontSize = 14.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp)
            }
        }
        if (theme.numeral != null) {
            LiquidMetalText(
                theme.numeral,
                style = TextStyle(
                    brush = gloss,
                    fontSize = 136.sp,
                    lineHeight = 128.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-4).sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 10f), blurRadius = 30f),
                ),
            )
            Text(theme.numeralCaption, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(theme.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        } else {
            LiquidMetalText(
                theme.title,
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    brush = gloss,
                    fontSize = 92.sp,
                    lineHeight = 84.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-3).sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 10f), blurRadius = 30f),
                ),
            )
        }
        theme.subtitle?.let {
            Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 17.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/**
 * Text filled with Paper's Liquid Metal shader (API 33+), the way Paper applies it to a logo:
 * the laid-out glyphs are rasterised once into a mask bitmap, which the shader's own
 * preprocessing turns into a bevel map. Below it sits the same text with a static chrome
 * gradient, which is what API 24-32 shows and what keeps the shape readable while the mask
 * is being processed.
 */
@Composable
private fun LiquidMetalText(text: String, style: TextStyle) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val padPx = with(LocalDensity.current) { MetalPad.roundToPx() }
    Box {
        val metalOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        Text(
            text,
            // With metal on top, the underlay only shows as a soft light halo that lifts the
            // darker metal bands off the black page.
            style = if (metalOn) style.copy(shadow = MetalHalo) else style,
            onTextLayout = { layout = it },
            modifier = Modifier.padding(MetalPad),
        )
        if (metalOn) {
            val metal = remember { ShaderState(LiquidMetalSpec, MetalPreset) }
            val mask = remember(layout) { layout?.let { textMask(it, padPx) } }
            if (mask != null && metal.image !== mask) metal.image = mask
            if (mask != null) PaperShader(metal, Modifier.matchParentSize())
        }
    }
}

/** White glyphs on transparent, with room around them for italic overhang and the bevel. */
private fun textMask(layout: TextLayoutResult, padPx: Int): Bitmap {
    val w = layout.size.width + 2 * padPx
    val h = layout.size.height + 2 * padPx
    val image = ImageBitmap(w, h)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, ComposeCanvas(image), Size(w.toFloat(), h.toFloat())) {
        drawText(layout, color = Color.White, topLeft = Offset(padPx.toFloat(), padPx.toFloat()), shadow = Shadow.None)
    }
    return image.asAndroidBitmap()
}

private val MetalPad = 12.dp

private val MetalHalo = Shadow(Color.White.copy(alpha = 0.30f), blurRadius = 36f)

private val MetalPreset = Preset(
    "Hero Metal",
    mapOf(
        "fit" to "contain",
        "scale" to 1.0f,
        "speed" to 1.0f,
        "useImage" to true,
        "colorBack" to color(0x00000000L),
        "colorTint" to color(0xffffffffL),
        "distortion" to 0.07f,
        "repetition" to 1.6f,
        "shiftRed" to 0.3f,
        "shiftBlue" to 0.3f,
        "contour" to 0.4f,
        "softness" to 0.1f,
        "angle" to 70.0f,
        "shape" to "none",
    ),
)

@Composable
private fun PrimaryButton(theme: HeroTheme) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(CircleShape)
            .background(theme.ctaColor)
            .clickable(role = Role.Button) {}
            .testTag("hero-cta"),
        contentAlignment = Alignment.Center,
    ) {
        Text(theme.cta, color = Color(0xFF16161A), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatRow(theme: HeroTheme) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(theme.statValue, color = theme.statColor, fontSize = 46.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Text(
                    theme.statUnit,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 9.dp),
                )
            }
            Text(theme.statLabel, color = if (theme.statColor == Color.White) Color.White.copy(alpha = 0.6f) else theme.statColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
            Text("Log meal", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BottomNav(theme: HeroTheme, modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val items = listOf(
        Icons.Filled.Home to "Home",
        Icons.Filled.FitnessCenter to "Workouts",
        Icons.Filled.Restaurant to "Meals",
    )
    Row(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
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
                Icon(icon, contentDescription = label, tint = if (on) Color.White else Color.White.copy(alpha = 0.7f))
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
    val ctaColor: Color,
    val badge: String?,
    val numeral: String?,
    val numeralCaption: String,
    val title: String,
    val subtitle: String?,
    val cta: String,
    val secondary: String,
    val statValue: String,
    val statColor: Color,
    val statUnit: String,
    val statLabel: String,
)

private val PageBlack = Color(0xFF0B0C10)

private const val EDGE_OFFSET_Y = 0.15f
private const val EDGE_SCALE = 0.55f

/** Keeps small white labels legible over the brightest parts of the gradient. */
private val LabelShadow = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 10f))

private fun warpPreset(name: String, vararg argb: Long, rotation: Float) = Preset(
    name,
    mapOf(
        "fit" to "none",
        "scale" to 1.0f,
        "rotation" to rotation,
        // Paper's own presets run at 3-20; below ~3 the warp barely drifts.
        "speed" to 5.0f,
        "offsetY" to EDGE_OFFSET_Y,
        "colors" to colors(*argb),
        "proportion" to 0.45f,
        "softness" to 1.0f,
        "distortion" to 0.2f,
        "swirl" to 0.5f,
        "swirlIterations" to 9.0f,
        "shapeScale" to EDGE_SCALE,
        "shape" to "edge",
    ),
)

val HeroThemes: List<HeroTheme> = listOf(
    HeroTheme(
        name = "Coral",
        preset = warpPreset("Hero Coral", 0xffff6a55L, 0xffef3a3fL, 0xffb01c30L, 0xff2e0a12L, 0xff0b0c10L, rotation = 0f),
        accent = Color(0xFFFF5A4E),
        ctaColor = Color(0xFFF6EEF2),
        badge = null,
        numeral = "5",
        numeralCaption = "moves",
        title = "Core & Glutes",
        subtitle = null,
        cta = "Start session",
        secondary = "Pick a different plan",
        statValue = "640",
        statColor = Color.White,
        statUnit = "KCAL",
        statLabel = "Energy balance",
    ),
    HeroTheme(
        name = "Neon",
        preset = warpPreset("Hero Neon", 0xffc4ff3aL, 0xff3fe04aL, 0xff0f8a32L, 0xff05240fL, 0xff0b0c10L, rotation = 0f),
        accent = Color(0xFFD4FF3A),
        ctaColor = Color(0xFFF2F2EC),
        badge = "Session complete",
        numeral = null,
        numeralCaption = "",
        title = "COOL\nDOWN",
        subtitle = "Stretch, hydrate, refuel",
        cta = "See recap",
        secondary = "Share your session",
        statValue = "312",
        statColor = Color(0xFFD4FF3A),
        statUnit = "KCAL",
        statLabel = "Burned today",
    ),
)
