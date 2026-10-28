# Universal USB Webcam

## Objective
Build an installable Android APK that streams a phone camera to OBS on Windows over USB, without DroidCam or vendor-specific APIs.

## Problem and why
The user wants to repurpose a Huawei P30 Pro as a webcam and reuse the APK on other Android devices. The current workspace has no Android app or SDK. A native USB-UVC gadget is not a portable application-level option, so OBS Browser Source plus ADB port forwarding is the accepted bridge.

## Authorized scope
- Create the Android application, focused automated tests, build files, and concise setup documentation.
- Install local Android build dependencies as needed and test the APK on the connected Huawei VOG-L04.
- Do not root, flash firmware, alter unrelated apps, expose the stream to the network, or install third-party webcam software on the phone.
- OBS is accepted on Windows. Do not push, open a PR, or deploy remotely.

## Constraints and design
- Portable across Android devices: use CameraX and Android APIs; minimum API 21, no Huawei dependencies.
- Foreground-only camera operation, user-visible controls, camera permission, loopback-bound HTTP MJPEG endpoint, bounded frame rate and resource use.
- USB access via `adb forward tcp:8765 tcp:8765`; OBS Browser Source loads a local page. No Wi-Fi exposure.
- Initial target: 720p at up to 15 fps, front/back camera choice where available. Video only.
- Strict TDD: enabled by project instructions. Test runner: `gradlew.bat testDebugUnitTest` once the Android project exists. Observe RED before implementation, then GREEN and REFACTOR.
- Delivery strategy: ask-on-risk. Forecast: approximately 380 authored changed lines for this minimal first version, excluding generated wrappers and local tools. Track actual lines after each work-unit commit; if the branch grows beyond about 400 lines, resolve chain strategy before the next commit.
- Engram mirror topic: `odd/universal-usb-webcam/tasks`; pending because no authoritative runtime session identity is currently registered. Do not claim mirror success.

## Tasks
- [x] **T1 — Android camera app and permission flow.** Route: delegated direct (multiple non-trivial files); implementation used Java and CameraX. RED: `gradlew.bat testDebugUnitTest` failed as expected with unresolved `CameraSelectionPolicy` before implementation. GREEN: same command passed. `gradlew.bat assembleDebug` and `gradlew.bat lintDebug` passed after setting targetSdk 35 (lint initially rejected targetSdk 28 as expired). Device smoke on Huawei VOG-L04 serial `45C0219917000594`: install succeeded; launcher opened; app camera permission prompt appeared and was granted for this app; live preview reached `Camera preview is running.` and displayed `STOP CAMERA`; stop control returned UI to `Camera preview stopped.`. Camera was stopped after smoke. Preview state was observed; visual image quality not assessed. Commit evidence: `c433f941fc57da7099f7f8811950057f2a62a22d` (`feat(webcam): add CameraX preview and permission controls`).
- [ ] **T2 — Loopback MJPEG transport.** Route: delegated direct; writer/preparation triggers. Add bounded frame encoding and a loopback-only HTTP endpoint with tested request handling and clean stop. Tests: RED/GREEN transport tests, unit suite, device HTTP smoke test through `adb forward`. Commit evidence: pending.
- [ ] **T3 — OBS integration and handoff.** Route: delegated direct if code changes require it; otherwise inline mechanical documentation. Add concise setup/rollback instructions and verify the APK on the Huawei, including a frame via host HTTP. If OBS is unavailable, report OBS visual playback as unverified rather than claiming success. Commit evidence: pending.

## Acceptance criteria
- APK installs and opens on the connected Android 10 Huawei, requests camera permission, and shows a live preview.
- Start/stop controls operate without background camera access; front/back switching behaves safely when a camera is absent.
- Localhost endpoint returns a bounded MJPEG stream over USB forwarding; no listener is exposed on Wi-Fi.
- Windows receives at least one valid JPEG frame through ADB forwarding.
- Build, unit tests, lint, and actual device smoke checks are reported honestly; OBS-specific playback is verified only if OBS is installed and tested.

## Progress and next step
- T1 implementation adds a permission-gated foreground CameraX preview with explicit start/stop, front/back switching when another lens is available, and a safe fallback to the front camera when no rear camera exists. Java was selected to avoid introducing a Kotlin plugin. Local JDK 17, Gradle 8.9, Android SDK platform/build-tools and command-line tools live under ignored `.local-tools/`.
- Exploration complete; CameraX plus loopback MJPEG/OBS selected as simplest portable architecture.
- Branch: `codex/universal-usb-webcam` (unborn; no commits yet).
- Next: implement T2 loopback MJPEG transport; T3 remains pending.
