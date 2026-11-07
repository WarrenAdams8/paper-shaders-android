# Changelog

## 0.1.1 (2026-11-07)

First release as a standalone library. (The 0.1.0 tag was withdrawn: its Gradle wrapper
checksum was wrong, so JitPack could not build it.)

- `:paper-shaders` Android library module, published through JitPack. The gallery is now a
  sample app that depends on it.
- 30 Paper shaders and 3 original interactive shaders, with Paper's presets.
- Compose: `PaperShader`, `Modifier.paperShader`, `rememberShaderState`, `ShaderClock`,
  `ShaderInput`, `TiltSensorEffect`.
- Views: `PaperShaderView` with the `paperShader` and `paperPlaying` XML attributes.
- `ShaderCatalog.byId` / `require`, `ShaderSpec.preset` / `param` / `allParams`,
  `ShaderState.applyPreset(name)`.
- `ShaderState.set` validates keys and value types, accepts any `Number` for numeric params and
  Compose `Color` for colors.
- Compose no longer needs `ShaderAssets.init(context)`; it initializes itself.
- No frame loop on the static pre-API 33 fallback.
- Legacy Warp-only API (`Warp`, `Modifier.warp`, `WarpView`) kept in `com.paperdesign.warp`.
