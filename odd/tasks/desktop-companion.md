# Portable desktop companion

## Objective
Deliver a portable Windows companion for GreenCam that pairs with the Android app, previews its MJPEG video, and provides camera controls and local filters without requiring the user to install a runtime.

## Problem and scope
The current Android app exposes an authenticated MJPEG URL that must be copied manually. There is no desktop client or control API. The user requested an easy PC pairing button, remote camera adjustments, filters, and a portable dependency-free executable.

## Constraints and decisions
- Windows x64 is the initial build target. A .NET 10 WinForms self-contained single-file publish needs no separately installed .NET runtime, at the cost of a larger executable. ARM64 can be built separately later.
- Pairing is explicitly initiated and approved on the phone. A short-lived, one-time code is bounded by expiration and failed-attempt limits. The session token is never displayed in the ordinary Android UI.
- Wi-Fi remains opt-in and bound to a concrete active Wi-Fi IPv4 address. Pairing and control state are revoked when the camera, Wi-Fi listener, or network address stops or changes.
- Existing HTTP is plaintext, not confidential against LAN interception. Use only a trusted local network. Do not claim TLS, UVC, or ADB-free USB.
- Android API 21 minimum; English artifact/UI baseline and neutral Spanish Android localization.
- Engram mirror `odd/desktop-companion/tasks` is pending because no authoritative runtime session identity is available.

## Tasks and acceptance
- [ ] D1 — Android pairing foundation: add a bounded pairing endpoint on the opt-in Wi-Fi listener, a time-limited one-use code and explicit phone approval. No unauthenticated camera control; failed attempts are limited. Preserve existing authenticated MJPEG and USB behavior. Checks: RED/GREEN `gradlew.bat testDebugUnitTest`; full `testDebugUnitTest assembleDebug lintDebug --offline --console=plain`; HTTP protocol tests; device smoke when available. Implementation and JVM socket tests complete; final full check and device smoke remain before checking off.
- [ ] D2 — Windows x64 portable companion: one self-contained single-file executable connects/pairs without manually exposing the bearer URL and previews MJPEG. Checks: desktop tests, `dotnet publish` self-contained/single-file, clean-machine runtime or equivalent dependency inspection, Android interop smoke.
- [ ] D3 — Authenticated camera controls: desktop can select exposed cameras/FPS and adjust supported zoom/focus through the paired session. Checks: Android protocol tests and desktop tests/device smoke.
- [ ] D4 — Local filters/output: offer user-facing filters and a practical desktop output path while preserving original stream behavior. Checks: desktop tests and visual/runtime smoke.

## Route and delivery
Delegated direct: Android pairing spans server, Activity, policy, tests, locale resources, and docs. Strict TDD is enabled by project instruction; runner is `gradlew.bat testDebugUnitTest`. Forecast for all four tasks exceeds 400 authored changed lines; deliver as sequential stacked-to-main work-unit PRs, each independently testable and under the review budget where feasible. RDD is disabled globally; ordinary functional checks still apply. Each task closes with a Conventional Commit on its feature branch, with tests and documentation alongside behavior.

## Progress
D1 in progress. Strict TDD RED: the new `PairingGateTest` failed compilation because `PairingGate` was absent; GREEN and full `testDebugUnitTest assembleDebug lintDebug --offline --console=plain` passed (48 tasks). A JVM socket test confirmed an invalid code receives 403, a valid code waits for phone approval, approval returns the bearer once, and replay receives 403. Runtime Android/PC interop remains pending; no connected-device action was performed in this work unit. The pairing listener uses a second concrete Wi-Fi-bound socket on port 8766; the existing protected MJPEG endpoint stays on 8765. Closing/rebinding the Wi-Fi listener rotates the bearer and revokes invitations. Rollback: revert the approval gate, pairing HTTP listener, Activity UI/lifecycle wiring, localized strings, and README note together. Gate-only preparatory commit: `7565fd9` (`feat(pairing): add one-time approval gate`). The initial product assumption is Windows x64; this does not imply the Android APK is architecture-specific.
