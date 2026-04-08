# Upgrade Readiness

This file records the current build baseline for the app and the concrete upstream documentation to review before changing libraries or the Nothing Glyph Matrix SDK.

## Current baseline

- Branch baseline: `chore/review-and-update-prep`
- Gradle wrapper: `9.3.1`
- Android Gradle Plugin: `9.1.0`
- Kotlin support: built in to AGP `9.1.0` (standalone `org.jetbrains.kotlin.android` plugin removed)
- compileSdk / targetSdk / minSdk: `36 / 36 / 34`
- Java / Kotlin JVM target: `17 / 17`
- Nothing SDK integration: refreshed local AAR at `app/libs/GlyphMatrixSDK.aar` sourced from upstream `glyph-matrix-sdk-2.0.aar`

## Concrete upstream docs to check before updates

### Nothing Glyph Matrix

- SDK repository: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
- SDK releases: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/releases
- Example project: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project

Relevant upstream changes already observed:

- Current docs list Phone (3) as `Glyph.DEVICE_23112`.
- Current docs add Phone (4a) Pro support as `Glyph.DEVICE_25111p` with a `13x13` matrix and AOD-only toy behavior.
- Current docs recommend using `Common.getDeviceMatrixLength()` for runtime sizing.
- Current docs add newer app-facing APIs such as `setAppMatrixFrame(...)` and `closeAppMatrix()` for supported system versions.
- Current releases page shows `V1.1` as latest and mentions behavior changes that require system version `20250801` or later, including updated app-matrix usage and brightness capped at `255`.

Upgrade implication:

- Replacing the local AAR is not a blind dependency bump. It can change device registration constants, matrix size assumptions, app-vs-toy display APIs, and system-version feature gates.

### Android Gradle Plugin

- Release notes index: https://developer.android.com/build/releases/gradle-plugin
- Past releases: https://developer.android.com/build/releases/past-releases
- AGP API updates: https://developer.android.com/build/releases/gradle-plugin-api-updates

Relevant upstream guidance already observed:

- Current AGP release notes are at `9.1.0`.
- AGP `9.1` requires JDK `17` and Gradle `9.3.1`.
- AGP `9.1` also introduces R8 behavior changes, including default repackaging when compiling to DEX.
- AGP `8.9.x` requires JDK `17`, Gradle `8.11.1`, and supports up to API level `35`.

Upgrade implication:

- This project is now on AGP `8.9.2` with Gradle `8.11.1`, so jumping to AGP `9.x` is still a separate platform upgrade, not a routine patch.
- Expect JDK changes, Gradle wrapper changes, and potential release-build behavior changes through R8.

### Gradle

- Compatibility matrix: https://docs.gradle.org/current/userguide/compatibility.html
- Release notes: https://docs.gradle.org/current/release-notes.html

Relevant upstream guidance already observed:

- Gradle `8.4` is the first release listed as supporting running on JDK `21`, but the compatibility matrix also notes later versions for broader support.
- Current Gradle docs list compatibility with newer Kotlin and AGP lines, but not all combinations are valid for older wrappers.

Upgrade implication:

- Wrapper upgrades should be staged with AGP upgrades, not treated independently.

### Kotlin

- Release overview: https://kotlinlang.org/docs/releases.html
- Kotlin evolution / compatibility guidance: https://kotlinlang.org/docs/kotlin-evolution-principles.html

Relevant upstream guidance already observed:

- Current Kotlin release line is `2.3.20`.
- Kotlin `2.x` includes the stable K2 compiler line and may require checking kotlinx library compatibility.

Upgrade implication:

- Moving from `1.9.22` to `2.x` should be handled separately from AGP jumps so build regressions can be isolated.

### Android platform behavior changes

- Android 15 behavior changes: https://developer.android.com/about/versions/15/behavior-changes-all
- Android 16 behavior changes: https://developer.android.com/about/versions/16/behavior-changes-all

Upgrade implication:

- If `targetSdk` changes as part of dependency modernization, re-check service, background execution, and any Nothing integration behavior against the relevant Android behavior-change pages.

## Recommended update order

1. Record the current local Nothing AAR filename/version before replacing it.
2. Upgrade AndroidX / Material / Retrofit / coroutines libraries that do not force build-tool changes.
3. Re-run build and device smoke tests.
4. Upgrade Kotlin in a controlled step.
5. Upgrade Gradle wrapper and AGP together only after confirming JDK requirements.
6. Refresh the Nothing AAR last, with a dedicated hardware validation pass.

Completed on this branch so far:

- Safe AndroidX/material/coroutines/lifecycle/datastore/test dependency refresh
- Kotlin migration to AGP 9 built-in Kotlin support
- Java and Kotlin JVM target upgrade to `17`
- Gradle wrapper upgrade to `9.3.1`
- Android Gradle Plugin upgrade to `9.1.0`
- compileSdk and targetSdk upgrade to `36`
- Nothing Glyph Matrix AAR refresh to upstream `glyph-matrix-sdk-2.0.aar`

## Current dependency ceiling on AGP 9.1 / API 36

- `androidx.core:core-ktx:1.18.0` builds successfully on the current `AGP 9.1.0` / `compileSdk 36` baseline.
- The following newer libraries were validated on the current baseline:
	- `androidx.core:core-ktx:1.18.0`
	- `androidx.appcompat:appcompat:1.7.1`
	- `com.google.android.material:material:1.13.0`
	- `androidx.constraintlayout:constraintlayout:2.2.1`
	- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
	- `androidx.lifecycle:lifecycle-runtime-ktx:2.10.0`
	- `androidx.datastore:datastore-preferences:1.2.1`

Implication:

- The project is now on the current stable AGP line and can consume the latest stable `core-ktx` release.

## Current validation status

- Full `build` succeeds on the current `AGP 9.1.0` / `API 36` baseline.
- `assembleDebug` succeeds on the current baseline.
- The generated debug APK installs and launches successfully on both the emulator and the Nothing phone.
- The previous Windows lint-cache lock was resolved by removing unused `lifecycle-viewmodel-ktx` and `lifecycle-livedata-ktx` dependencies, then declaring the actually used `lifecycle-runtime-ktx` dependency explicitly.
- The refreshed Nothing AAR builds without source changes, and the app still launches on the Nothing phone with no new Glyph SDK linkage errors observed in filtered logcat.

## Remaining Nothing SDK follow-up

- The project now uses runtime matrix sizing through `Common.getDeviceMatrixLength()` and runtime device registration for Glyph toy service startup.
- Compact `13x13` rendering is implemented for Phone (4a) Pro by reducing text density in favorite-team, record, rankings, division, error, and debug-array displays.
- The compact path has not yet been validated on real Phone (4a) Pro hardware in this repo.

## Must-retest areas after each step

- App launch on device
- Manual refresh of standings
- Favorite team lookup and division lookup
- Glyph toy activation from Nothing settings
- Long-press mode cycling
- AOD callback behavior