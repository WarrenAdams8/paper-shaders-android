# Paper Design Warp Shader for Android

A high-performance Android rebuild of the **Paper Design Warp Shader** ([shaders.paper.design/warp](https://shaders.paper.design/warp)) using Android Graphics Shading Language (**AGSL**), `android.graphics.RuntimeShader`, and **Jetpack Compose**.

Runs 100% on the GPU with zero per-frame heap allocations, supporting fluid domain warping, layered vortex swirls, procedural noise, and geometric base patterns (Checks, Stripes, Edge).

---

## Features

- **Pixel-Accurate Port**: Rebuilds the exact WebGL fragment shader math from `@paper-design/shaders` in AGSL (SkSL).
- **Jetpack Compose Native**: Provides `@Composable fun Warp(...)` and `Modifier.warp(...)`.
- **Zero-Allocation Render Loop**: Uniform buffers and compiled `RuntimeShader` instances are cached across frames.
- **6 Official Paper Design Presets**:
  - `Default` (signature purple fluid checks)
  - `Cauldron Pot` (mystical green/blue swirling edge)
  - `Live Ink` (monochromatic marbled ink strokes)
  - `Kelp` (aquatic wavy stripes)
  - `Nectar` (warm honey & cream liquid split)
  - `Passion` (fiery crimson & peach vortex)
- **Classic Android View**: Includes `WarpView` for XML-based layouts and legacy views.
- **Backward Compatible**: Leverages hardware AGSL on Android 13+ (API 33+) with an automatic smooth radial gradient fallback on Android 12 and below.
- **Zero Asset Dependencies**: Uses high-performance procedural value noise (`hash21`), requiring no external textures or image assets.

---

## Mathematical Architecture

The AGSL shader faithfully reproduces the original Paper Design pipeline:

1. **Normalized Pattern Space**:
   $$\text{uv} = \left(\mathbf{R}(\theta) \cdot \frac{\text{fragCoord} - 0.5 \cdot \text{res}}{\text{scale}} - \text{offset} \cdot \text{minDim}\right) \cdot \frac{2.5}{\text{minDim}}$$
2. **Noise Domain Warping**:
   Two value-noise octaves evaluate continuous fluid displacement:
   $$\text{angle} = n_1 \cdot 2\pi, \quad \Delta \text{uv} = 4 \cdot \text{distortion} \cdot n_2 \cdot (\cos(\text{angle}), \sin(\text{angle}))$$
3. **Layered Swirl Passes**:
   Accumulates up to 20 harmonic vortex rotations:
   $$\text{uv}_x \mathrel{+}= \frac{\text{swirl}}{i} \cos(t + 1.5 i \cdot \text{uv}_y), \quad \text{uv}_y \mathrel{+}= \frac{\text{swirl}}{i} \cos(t + 1.0 i \cdot \text{uv}_x)$$
4. **Base Pattern Evaluation**:
   - **Checks**: $0.5 + 0.5 \sin(\text{uv}_x) \cos(\text{uv}_y) + \text{bias}(\text{proportion})$
   - **Stripes**: $\text{smoothstep}(0.0, 0.55, \{ \text{uv}_y \}) \cdot (1 - \text{smoothstep}(0.45, 1.0, \{ \text{uv}_y \}))$
   - **Edge**: $\text{smoothstep}(e_0, e_1, 1.0 - \text{uv}_y + 0.3(\text{proportion} - 0.5))$
5. **Anti-Aliased Color Mixing & Dithering**:
   Blends up to 10 RGBA color stops using `fwidth()` hardware derivative smoothing, followed by spatial high-frequency triangular dither to eliminate color banding on mobile OLED displays.

---

## Quick Start

### 1. Jetpack Compose Composable

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paperdesign.warp.Warp
import com.paperdesign.warp.WarpPresets

@Composable
fun MyScreen() {
    Warp(
        modifier = Modifier.fillMaxSize(),
        params = WarpPresets.Default
    )
}
```

### 2. Apply as a Background Modifier

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.paperdesign.warp.WarpPresets
import com.paperdesign.warp.warp

Box(
    modifier = Modifier
        .fillMaxSize()
        .warp(params = WarpPresets.CauldronPot),
    contentAlignment = Alignment.Center
) {
    Text("Warp Background")
}
```

### 3. Custom Parameter Configuration

```kotlin
val customParams = WarpParams(
    colors = listOf(
        Color(0xFF121212),
        Color(0xFF9470FF),
        Color(0xFF8838FF)
    ),
    proportion = 0.45f,
    softness = 1.0f,
    distortion = 0.25f,
    swirl = 0.8f,
    swirlIterations = 10,
    shape = WarpShape.Checks,
    shapeScale = 0.1f,
    speed = 1.0f,
    scale = 1.0f,
    rotation = 0.0f
)

Warp(params = customParams)
```

### 4. Traditional XML Layout (`WarpView`)

```xml
<com.paperdesign.warp.view.WarpView
    android:id="@+id/warpView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
val warpView = findViewById<WarpView>(R.id.warpView)
warpView.params = WarpPresets.LiveInk
warpView.isPlaying = true
```

---

## Project Structure

```
warp-shader-android/
├── app/src/main/kotlin/com/paperdesign/warp/
│   ├── Warp.kt                # Compose Composable & Modifier.warp()
│   ├── WarpShader.kt          # AGSL shader code & uniform update utilities
│   ├── WarpParams.kt          # Configuration data class
│   ├── WarpPresets.kt         # Official Paper Design presets
│   ├── WarpShape.kt           # Checks, Stripes, Edge enum
│   ├── view/
│   │   └── WarpView.kt        # Android View for XML layouts
│   └── demo/
│       └── WarpPlayground.kt  # Full interactive showcase UI with controls
└── app/src/test/kotlin/com/paperdesign/warp/
    └── WarpShaderTest.kt      # Unit tests for shader integrity & presets
```

---

## Building and Testing

```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```
