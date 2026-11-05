# Paper Shaders for Android

An AGSL port of all 30 shaders from [paper-design/shaders](https://github.com/paper-design/shaders)
(Apache-2.0), with a gallery app for trying every preset and parameter live.

## Gallery

- A grid of all shaders (static thumbnails). Tap one to open it full screen.
- The controls sheet (tune icon) holds Paper's presets, every shader parameter (sliders, options,
  switches, color pickers, color lists), and the shared sizing and motion params (fit, scale,
  rotation, offset, origin, world size, speed, frame).
- Image shaders use a bundled sample image. Use **Pick image** to load your own via the photo picker.
- The header shows the live frame rate for animated shaders.

Open a shader directly: `adb shell am start -n com.paperdesign.warp/com.example.warpshader.MainActivity --es shader warp`

## Using a shader in your own Compose UI

```kotlin
val state = remember { ShaderState(MeshGradientSpec) }   // or ShaderState(spec, spec.presets[2])
PaperShader(state, Modifier.fillMaxSize())
state.set("distortion", 0.6f)                             // live edits only touch uniforms
```

## How the port works

| Upstream (WebGL2) | Android (AGSL) |
|---|---|
| Vertex shader computes object/pattern/responsive/image UVs | `PaperSizing` runs the same math on the CPU and sends each UV as two affine rows |
| `u_noiseTexture` (128x128 PNG) | `res/drawable-nodpi/paper_noise.png` bound with `setInputBuffer`, CLAMP + bilinear |
| `texture(u_image, uv)` | `paper_image(uv)` = `u_image.eval(uv * size)` |
| `fwidth`, `dFdx`, `dFdy` (not in AGSL) | Exact affine derivatives, analytic gradients, or forward differences at neighbour pixels (only when softness is near 0 for the expensive ones). Each site is commented in the `.agsl` file |
| `%`, array constructors, dynamic indexing, `round`, `transpose`, `bool` uniforms | Rewritten with `mod`, arithmetic tables (Bayer), loop lookups, `floor(x + .5)`, inverse rotation, float uniforms |
| JS image preprocessing (heatmap blurs, Poisson roundness for Liquid Metal / Gem Smoke) | `ImageProcessing.kt`, run on `Dispatchers.Default` and cached |

Rendering cost: uniforms are pushed only after an edit or a resize; per frame only `u_time` is
written, from a single reused frame callback, and state is read in the draw phase so animation
does not recompose.

Android 12 and lower (API 24-32) have no AGSL: shaders fall back to a radial gradient of their
colors, and image shaders show the image.

## Layout

```
app/src/main/assets/shaders/*.agsl            shader bodies (ported fragment shaders)
app/src/main/kotlin/com/paperdesign/shaders/
  ShaderSpec.kt, ShaderState.kt               params, presets, live values
  ShaderProgram.kt, ShaderPrelude.kt          RuntimeShader + uniform upload, generated header
  PaperSizing.kt                              CPU port of Paper's vertex shader
  ImageProcessing.kt, ShaderAssets.kt         image preprocessing, noise texture, sample images
  PaperShader.kt                              Compose renderer + fallback
  specs/                                      one spec per shader + ShaderCatalog
  gallery/                                    gallery, playground, controls, color picker
```

## Tests

- `./gradlew :app:testDebugUnitTest`: sizing math, and spec/asset consistency (uniforms declared, no GLSL-only features, presets valid).
- `./gradlew :app:connectedDebugAndroidTest`: compiles every shader and renders every preset on a device (API 33+).
- `benchmark` build type: release-like, signed with the debug key, for measuring frame rates.

---

The original single-shader Warp library (`com.paperdesign.warp`) is still included and documented below.

## Paper Design Warp Shader (original library)

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
