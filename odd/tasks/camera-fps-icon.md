# Per-Camera FPS Limits and Launcher Icon

## Objective
Show the selected camera's advertised frame-rate limit on every Android device and give GreenCam a distinctive G/C launcher icon.

## Problem and scope
The current UI offers software output caps of 10/15/30/60 FPS without checking each camera's capabilities. On the tested Huawei, a 60 FPS cap delivered about 25 FPS and standard Camera2 AE target ranges topped out at 30. This can mislead users. Read the actual selected camera's standard capture-rate ranges, show their advertised maximum, and disable output-cap choices above it when known. Keep the difference between advertised camera capability and measured MJPEG throughput explicit. If capability data is unavailable, show "Unknown" and retain existing controls rather than inventing a limit.

Create a minimal vector launcher icon with a G and C separated by a diagonal cut, supporting Android API 21+ and adaptive launchers on API 26+. The user requested frame interpolation toward 60 FPS as future work; record it as a backlog idea only. It is NOT part of this change and must not be represented as real capture rate.

## Constraints and route
- Preserve CameraX 1.4.1, minSdk 21, the USB/OBS transport, and foreground-only camera operation.
- Use the actual selected/bound `CameraInfo` for Camera2 AE target ranges, not a device-wide assumption. The range maximum is advertised capability, not a promised output rate.
- Strict TDD enabled by project instructions: `gradlew.bat testDebugUnitTest`; observe RED before behavior implementation, then GREEN. Run `assembleDebug`, `lintDebug`, and a device smoke check when feasible.
- Route: delegated direct. Mapping required 4+ files; implementation spans multiple non-trivial files. T1 and T2 each get a work-unit commit.
- Forecast: about 380 authored changed lines, not a cap or code-golf target. Delivery uses the previously selected `stacked-to-main` strategy; split coherent PR slices if the accumulated diff exceeds 400 lines. Remote destination/credential scope remains the user-authorized `menagreen/GreenCam` GitHub CLI session for push, sequential PRs, and APK release.
- Engram recovery mirror `odd/camera-fps-icon/tasks` is pending because no authoritative runtime session identity is available.

## Tasks
- [x] **T1 — Per-camera FPS capability.** Added pure-Java policy tests for advertised range maxima, unknown metadata, disabled caps, and selection fallback; wired CameraX/Camera2 lookup to the selected or bound camera and updated UI/README. RED observed before implementation; GREEN, build, lint, and diff checks passed. Device smoke remains pending parent delegation. Route: delegated direct writer; mapping trigger: understanding crossed 4 files; writer trigger: behavior spans MainActivity, policy/tests, and README. Commit evidence: `a9887e633c33f834196f9deedc1a25a847d5ef38`.
- [ ] **T2 — G/C launcher icon.** Create vector legacy/adaptive launcher resources with G and C divided by a diagonal cut, set the manifest icon, visually inspect render and installed launcher icon, and commit. Route: delegated writer.
- [ ] **T3 — Delivery.** Verify each work unit and cumulative PR size, merge reviewable PR slice(s), build from merged `main`, and publish a matching debug-signed prerelease APK. Route: inline Git/release orchestration with delegated checks.

## Acceptance criteria
- Switching front/rear updates the displayed advertised FPS maximum for that camera; 60 is disabled when the known maximum is below 60. Unknown metadata is labeled unknown and never treated as proof of support.
- Switching cameras safely reconciles a previously selected output cap and preserves preview/stream behavior.
- App icon is distinct, legible at launcher size, and appears on the tested Android 10 device; API 21+ has a valid fallback.
- Tests/build/lint and feasible device checks have observed, honestly reported outcomes.

## Progress
- Exploration confirmed CameraX `CameraInfo` and Camera2's `CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES` can identify a per-camera advertised range with the current library. T2 icon work remains untouched.
- T1 verification: first `gradlew.bat testDebugUnitTest` failed at test compilation because `FpsCapabilityPolicy` did not yet exist (expected RED). After implementation, `gradlew.bat testDebugUnitTest` passed. Full `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain` passed using the local JDK 17 and Android SDK; lint reported 19 warnings and no errors. `git diff --check` passed. Device smoke is intentionally pending; no device/OBS actions were authorized.
- Future backlog: optional frame interpolation to a 60 FPS output, with an explicit latency/quality/performance evaluation; not authorized as current implementation.
- Branch: `codex/camera-fps-icon`. Next: T1 strict-TDD implementation.
