# Glyph Matrix SDK

This directory now contains the refreshed `GlyphMatrixSDK.aar` copied from Nothing's upstream `glyph-matrix-sdk-2.0.aar` artifact.

## Current local state:

1. Active SDK file: `app/libs/GlyphMatrixSDK.aar`
2. Source artifact: `glyph-matrix-sdk-2.0.aar` from the upstream repository
3. Backup of the previous local AAR: `app/libs/temp/GlyphMatrixSDK-pre-refresh.aar`
4. The module build is configured to keep using the stable local filename `libs/GlyphMatrixSDK.aar`

## SDK Documentation:
- Main repository: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
- Example project: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project
- Releases / change log: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/releases

## Current compatibility notes

- The local project now compiles against `Glyph.DEVICE_23112`, which aligns with the current upstream Phone (3) identifier documented in the Nothing kit.
- Upstream documentation also describes `Glyph.DEVICE_25111p` for Phone (4a) Pro and a 13x13 matrix path, so any future SDK refresh should re-check device targeting and matrix-size assumptions before release.
- The refreshed upstream AAR adds newer manager APIs such as `setAppMatrixFrame(...)` and `closeAppMatrix()`, but the existing Glyph Toy service calls used by this app remain source-compatible.
- The app now uses `Common.getDeviceMatrixLength()` and runtime target selection to adapt its own rendering path for both `25x25` and compact `13x13` devices.
- The new compact rendering mode is intended for Phone (4a) Pro, but it has not yet been validated on real Phone (4a) Pro hardware.
