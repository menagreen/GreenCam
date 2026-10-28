# Camera Controls and UI

## Objective
Make camera selection and frame-rate control clear and usable in GreenCam without changing its USB/OBS transport.

## Problem and scope
The app already toggles front/back cameras, but does not show the active camera. Its output frame pacing is hard-coded to 15 FPS. Add explicit front/rear selection, live 10/15/30 FPS output caps, and a clearer foreground control layout. Camera hardware may deliver fewer frames than the selected cap; do not promise an exact capture rate. Additional physical rear lenses, audio, background capture, and network streaming are out of scope.

## Constraints
- Preserve Android API 21 support, CameraX portability, foreground-only operation, and loopback-only MJPEG.
- Strict TDD is enabled by project instructions; runner: `gradlew.bat testDebugUnitTest`. Observe RED before implementation, then GREEN and refactor. Run `assembleDebug`, `lintDebug`, and focused device checks when available.
- Route: delegated direct. Mapping covered at least four files; implementation requires multiple non-trivial files.
- Delivery: existing `stacked-to-main` preference; forecast about 320 authored changed lines, monitor the 400-line review threshold and split by work unit if needed. PR/push only under the user's prior authorization for `menagreen/GreenCam` through the authenticated GitHub CLI session.
- Engram mirror topic `odd/camera-controls-ui/tasks` is pending: no authoritative runtime session identity is registered.

## Tasks
- [x] **T1 — Testable live FPS cap.** Added a synchronized `FrameRateLimiter` with 10/15/30 FPS caps, default 15, live cap changes, and session reset; the image analyzer reads it before JPEG encoding. RED: `gradlew.bat testDebugUnitTest --offline --console=plain` failed to compile the new test because `FrameRateLimiter` did not exist (6 errors, exit 1). GREEN: the same test command passed (20 tasks, exit 0). Full `testDebugUnitTest assembleDebug lintDebug --offline --console=plain` passed (48 tasks, exit 0), and `git diff --check` passed. Runtime device harness: N/A for this internal pacing unit; live control/device stream check belongs to T2. Rollback boundary: limiter, its tests, and `MainActivity` pacing wiring only. Route: delegated writer. Commit identity recorded below.
- [x] **T2 — Explicit camera and FPS controls.** Added a scrollable labeled preview/control layout, explicit rear/front radio selection with availability and active-choice summary, live 10/15/30 FPS cap selection, and concise OBS help. Selection policy tests cover unavailable-camera retention and default fallback. RED: `gradlew.bat testDebugUnitTest --offline --console=plain` failed compilation on four missing policy-method references (exit 1); GREEN: the same command passed (20 tasks, exit 0). Full `testDebugUnitTest assembleDebug lintDebug --offline --console=plain` passed (48 tasks, exit 0); `git diff --check` passed. README describes new controls and cap semantics. Device/OBS runtime smoke is pending parent delegation; do not infer it from unit tests. Rollback boundary: camera policy/test additions, `MainActivity` UI/selection wiring, and README controls/release-link update. Route: delegated writer. Commit identity recorded below.
- [ ] **T3 — Delivery and handoff.** Review cumulative diff and checks, keep PR slices reviewable, push/merge the authorized PR(s), build from merged `main`, and publish a matching prerelease APK if verification passes. Route: inline Git/release orchestration with delegated execution checks.

## Acceptance criteria
- User can see and choose the available front/rear camera and change it while running.
- User can choose an output cap of 10, 15, or 30 FPS while running; default remains 15. The UI says "up to" rather than guaranteeing camera hardware rate.
- Preview, USB stream, permission, stop/restart, and unavailable-camera behavior remain functional.
- Unit tests, APK build, lint, and feasible device/OBS smoke checks have observed outcomes.

## Progress
- Exploration: `MainActivity` already toggles front/back via `CameraSelectionPolicy`; the UI has no active-lens indicator. `MIN_FRAME_INTERVAL_NANOS` enforces a fixed 15 FPS output cap in `analyzeFrame`.
- Current branch: `codex/camera-controls-ui`. T1 work-unit commit: `5fba010b4f5aa38e4ac553c12d331d4617c68aef`. T2 work-unit commit: `bdb7498069984ca165407e861f9d57c34d19893e`. T2 implementation is complete; next: verify UI and stream on device, then deliver. README points to the planned `v0.1.0-alpha.2` prerelease; release metadata must match before publishing.
