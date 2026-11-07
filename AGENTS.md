# AGENTS.md

Guide for coding agents. Part 1 is for agents **using** this library in another app. Part 2 is
for agents **changing** this repository.

## Part 1: Using the library in an app

Dependency (JitPack, add `maven("https://jitpack.io")` to the repositories in `settings.gradle.kts`):

```kotlin
implementation("com.github.WarrenAdams8:paper-shaders-android:<version>")
```

The smallest working use:

```kotlin
val state = rememberShaderState(ShaderCatalog.require("mesh-gradient"))
PaperShader(state, Modifier.fillMaxSize())
```

Facts to rely on:

- Shader ids, parameter keys, types, ranges and preset names are all in `docs/SHADERS.md`. Do not
  guess keys: `ShaderState.set` throws `IllegalArgumentException` on an unknown key or a wrong type.
- Numeric params accept any `Number`. Enum params take a `String` option (`"checks"`). Colors take
  `ArgbColor(0xAARRGGBB)` or a Compose `Color`. Color lists must have 1 to `maxCount` entries.
- Every shader also accepts the common params `fit`, `scale`, `rotation`, `offsetX`, `offsetY`,
  `originX`, `originY`, `worldWidth`, `worldHeight`; animated shaders also take `speed`, `frame`.
- `Modifier.paperShader(state)` draws behind the element's content. `PaperShader(...)` is just a
  `Box` with that modifier.
- Create a `ShaderState` once (`rememberShaderState`) and mutate it. Do not create one per
  recomposition: each new spec compiles a new `RuntimeShader`.
- `playing = false` or `speed = 0` freezes a shader. `ShaderClock.timeMs` controls time directly.
- Image shaders read `state.image`, a software `Bitmap` (not `Config.HARDWARE`). Without one they
  use a bundled sample.
- API 24-32 has no AGSL. Shaders then draw a static gradient of their colors. Do not add your own
  version checks around `PaperShader`; it handles them.
- XML: `com.paperdesign.shaders.view.PaperShaderView` with `app:paperShader` and `app:paperPlaying`.
- `ShaderAssets.init(context)` is only needed if you use `ShaderAssets` or `ShaderProgram` directly
  before any `PaperShader` or `PaperShaderView` exists.

## Part 2: Working on this repository

### Layout

- `paper-shaders/`: the library. Namespace and package `com.paperdesign.shaders`, plus the legacy
  Warp-only API in `com.paperdesign.warp`.
  - `src/main/assets/shaders/<id>.agsl`: one shader body per spec.
  - `src/main/kotlin/com/paperdesign/shaders/specs/<Name>Spec.kt`: parameters and presets;
    `ShaderCatalog.kt` lists them all.
  - `ShaderPrelude.kt` prepends uniforms and helpers that a body references (`paper_objectUV`,
    `paper_patternUV`, `paper_imageUV`, `paper_noise`, `paper_image`, `paper_round`, `u_pointer`,
    `u_touches`, `u_tilt`, ...). Only referenced symbols are declared.
  - `ShaderProgram.kt` compiles and uploads uniforms; `PaperSizing.kt` is the CPU port of Paper's
    vertex shader; `PaperShader.kt` is the Compose renderer and fallback.
- `app/`: sample gallery (`com.example.warpshader` namespace, application id
  `com.paperdesign.warp`). It depends on `project(":paper-shaders")`. Nothing in the library may
  depend on the app.
- `docs/SHADERS.md`: generated. Never edit it by hand.

### Commands

```bash
./gradlew :paper-shaders:testDebugUnitTest          # all JVM unit tests
UPDATE_DOCS=1 ./gradlew :paper-shaders:testDebugUnitTest   # regenerate docs/SHADERS.md
./gradlew :paper-shaders:lintDebug :app:lintDebug   # reports in <module>/build/reports/lint-results-debug.txt
./gradlew :paper-shaders:connectedDebugAndroidTest  # device tests; set ANDROID_SERIAL with several devices
./gradlew :app:connectedDebugAndroidTest            # sample smoke test
./gradlew :paper-shaders:publishToMavenLocal        # check the published AAR and POM
```

JDK 17, AGP 9 (built-in Kotlin: there is no `kotlin-android` plugin), compileSdk 37, minSdk 24.

### Definition of done

1. Unit tests pass and `docs/SHADERS.md` is current (the `ShaderReferenceDocTest` fails otherwise).
2. Lint has no new warnings in either module (dependency-version warnings excepted).
3. Any shader or rendering change has been run on a device: API 33+ for AGSL, and API 32 or lower
   for the fallback. A Compose preview does not execute `RuntimeShader`, so look at a real
   screenshot (`adb exec-out screencap -p > out.png`).
4. Say which of these you ran and which you skipped.

### AGSL rules

AGSL is not GLSL. `ShaderCatalogTest` rejects these in `.agsl` bodies: `fwidth(`, `dFdx(`,
`dFdy(`, `texture(`, `textureLod(`, `textureGrad(`, `textureSize(`, `#define`, `sampler2D`,
`gl_FragCoord`, `transpose(`. Also avoid:

- `%` on ints (use `mod`), `round` (use `paper_round`), `bool` uniforms (use float 0/1).
- Array constructors and indexing arrays with non-constant expressions (loop over a constant
  bound and compare the index instead).
- Loops with a non-constant bound: use a constant maximum and `if (i >= n) break;`.
- Derivatives: compute them analytically or with forward differences, and comment why.

Compile errors only show up at runtime. Check logcat for `AGSL compile failed for <id>` (tag
`PaperShader`), or run `ShaderCompileTest`, which compiles every shader and renders every preset.

### Adding a shader

1. Add `src/main/assets/shaders/<id>.agsl` with `half4 main(float2 fragCoord)`, using the prelude
   helpers for UVs, noise and images.
2. Add `<Name>Spec.kt` in `specs/`: `id` must equal the file name, every `ParamDef.uniform` must be
   declared in the body, presets may only use the spec's keys and the common keys, and the first
   preset is the default.
3. Register it in `ShaderCatalog` (`paper` for ports, `originals` for new designs).
4. Regenerate the docs, run the unit tests, then run `ShaderCompileTest` on an API 33+ device.
5. For ports, keep the upstream credit comment ("Port of paper-design/shaders ... (Apache-2.0)").

### Rendering rules

- Never allocate in draw or per frame. Uniform arrays are preallocated; `ShaderState.version` and
  `ShaderInput.version` are read inside the draw lambda so edits redraw without recomposing.
- Uniforms are uploaded only when `version` or the size changes; per frame only `u_time` is set.
- Keep public API changes deliberate: this is a published library. Note them in `CHANGELOG.md`.

### Device testing notes

- Compose UI tests on API 36+ need espresso 3.7+ (the library pins it in `androidTestImplementation`).
- Animated shaders keep Compose busy forever. In UI tests, set `mainClock.autoAdvance = false`
  and advance the clock by hand, or pass `playing = false`.
- A locked phone makes Compose tests fail with "No compose hierarchies found". Unlock it or use an
  emulator.

### Releasing

1. Update `VERSION_NAME` in `gradle.properties` and the version in `README.md` and `CHANGELOG.md`.
2. Commit, tag with the bare version (`git tag 0.2.0`), and push the tag.
3. JitPack builds on first request (`jitpack.yml` publishes only `:paper-shaders`). Check
   `https://jitpack.io/#WarrenAdams8/paper-shaders-android` for the build log.

### License

Apache-2.0. Ported code derives from paper-design/shaders (Apache-2.0); keep `NOTICE` intact and
keep crediting Paper in the README.
