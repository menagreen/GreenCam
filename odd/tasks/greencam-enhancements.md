# GreenCam Enhancements

## Objective
Deliver the requested GreenCam backlog in reviewable work units while preserving Android API 21 compatibility and avoiding new permissions/services unless separately authorized.

## Problem and why
The user requested multiple camera and usability improvements. The first selected work unit is a user-reversible black, minimum-brightness screen mode that leaves the app foregrounded and camera/stream active. This is explicitly not physical display-off or background-service behavior.

## Scope and constraints
- T1: add black screen mode with minimum window brightness; keep camera/stream active; accessible user exit; restore brightness on exit, `onStop`, and destroy; prevent bright static controls/burn-in; update concise README guidance.
- Later, pending: Wi-Fi; ADB-free transport; Windows desktop companion/filters; physical rear lenses; zoom/focus; dark mode; menu/aesthetic UI; multilingual support.
- No new permissions/services for T1 dim mode; no physical display-off; Android API 21 minimum; English artifact language. T2b Wi-Fi is explicitly authorized to add only the normal `ACCESS_NETWORK_STATE` permission; no location permission or background service.
- No device/OBS changes, push, PR, or release.

## Route and evidence
- Route: delegated direct; parent assigned a bounded writer.
- Trigger: implementation is a multi-file behavior change (Activity, focused tests, README, task record).
- TDD: enabled by project instruction; runner: `gradlew.bat testDebugUnitTest`; RED before behavior, then GREEN/refactor.
- Checks: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain`; `git diff --check`.
- Delivery strategy: ask-on-risk; no PR strategy applies in this work unit.
- Forecast: under 400 authored changed lines.

## Acceptance criteria
- User can enable and exit the black mode without leaving the app.
- While active, window brightness is set to minimum and screen content is black; controls do not remain brightly lit.
- Camera preview and MJPEG stream remain active.
- Original brightness is restored on explicit exit and lifecycle cleanup (`onStop`/destroy).
- Tests demonstrate the relevant mode/brightness policy, and README describes the feature accurately.

## Tasks
- [x] T1 — Implement reversible black/minimum-brightness screen mode, tests, and README. RED observed before implementation (unit-test compile failed because `ScreenDimPolicy` was absent); GREEN observed with `testDebugUnitTest` in the full check below.
- [x] T2a — Add transport-independent, per-session token authentication for the future listener. USB loopback remains unauthenticated and unchanged; Wi-Fi listener must configure token protection for index and stream routes. Commits `93a41c8`, `7d5634b`.
- [x] T2b — Add an opt-in Wi-Fi listener/UI while camera runs. Resolve only a concrete active Wi-Fi IPv4 address; bind a separate server socket to that address (never `0.0.0.0`) and share the existing frame store. Protect both routes with a fresh 128-bit per-run authenticator token. Use API 21 `ConnectivityManager` network callbacks and `LinkProperties` to close/rebind on availability, loss, or IP changes. Display a selectable/copyable OBS URL only while Wi-Fi is enabled and available, with a trusted-LAN-only warning. Stopping the camera closes Wi-Fi and a subsequent start rotates the token. Preserve the USB listener at `127.0.0.1:8765` untouched. Permission delta is only normal `ACCESS_NETWORK_STATE`; no location or background service. Pure IPv4-selection and listener lifecycle policy tests passed; Authored diff: 365 lines (359 additions, 6 deletions).
- [ ] T3 — ADB-free transport. Pending user backlog; not scoped or implemented.
- [ ] T4 — Windows desktop companion/filters. Pending user backlog; not scoped or implemented.
- [ ] T5 — Physical rear lenses. Pending user backlog; not scoped or implemented.
- [ ] T6 — Zoom/focus. Pending user backlog; not scoped or implemented.
- [ ] T7 — Dark mode. Pending user backlog; not scoped or implemented.
- [ ] T8 — Menu/aesthetic UI. Pending user backlog; not scoped or implemented.
- [ ] T9 — Multilingual support. Pending user backlog; not scoped or implemented.

## Progress and next step
T1 implementation complete. Verification: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain` succeeded; `git diff --check` succeeded. Runtime/device harness: Huawei VOG-L04 smoke passed. The overlay covered 99.82% of the app area with pixels below RGB 20; Android window brightness reported `sbrt=0.0`; a valid JPEG streamed while dimmed. Tap restored controls/default brightness, and Stop closed the stream. Device luminance was not measured. Rollback boundary: revert the single T1 commit to remove the dim UI, brightness handling, policy tests, and documentation together. Commit identity: `e2d6221` (`feat(camera): add reversible dim screen mode`). Engram mirror pending (no authoritative session identity available and parent requested no Engram writes).

T2a route: delegated direct (bounded writer). Trigger evidence: auth policy, parser, and unit tests form a multi-file behavior change; CodeGraph mapped `MjpegHttpServer` request routing and its test callers before edits. Strict TDD is enabled; runner: `gradlew.bat testDebugUnitTest`. RED observed first: test compilation failed because `MjpegRequestAuthenticator` did not exist. Implemented a 128-bit `SecureRandom` lowercase-hex session token, strict single `token` query parsing, `MessageDigest.isEqual` credential comparison, optional protected-server enforcement returning 400 for malformed targets and 401 for absent/incorrect credentials, and tests for index + stream protection. Independent verification identified the root cause: the protected index used the static USB page, so its image URL omitted the token required by the protected stream endpoint. Added a failing end-to-end test (index HTML did not contain the session token; `testDebugUnitTest --tests ...protectsIndexAndStreamOnlyWhenAuthenticatorIsConfigured` failed at that assertion), then changed only the protected index response to insert the validated lowercase-hex token in the stream URL; unprotected USB continues to use its original static page. Focused GREEN: `gradlew.bat testDebugUnitTest --tests com.example.usbwebcam.MjpegHttpServerTest.protectsIndexAndStreamOnlyWhenAuthenticatorIsConfigured --offline --console=plain` succeeded. Full verification also succeeded: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain`; `git diff --check` succeeded. Runtime/device harness: N/A; no listener binds in T2a. Rollback boundary: revert the T2a auth/parser behavior, its tests, and task progress; does not remove USB compatibility. Commits: `93a41c8` (`feat(camera): add optional session token authentication`), `7d5634b` (`fix(camera): include session token in protected index`). T2a auth/token behavior was completed in commits `93a41c8` and `7d5634b`; the Wi-Fi listener/UI was completed separately in T2b.

T2b route: delegated direct (multi-file listener/UI, permission, policy tests, README, and task record); CodeGraph mapped the camera lifecycle and server request routing before edits. Strict TDD is enabled; runner: `gradlew.bat testDebugUnitTest`. RED observed before implementation: compiling `WifiNetworkPolicyTest` failed because `WifiNetworkPolicy` did not exist. GREEN: policy tests verify concrete normalized IPv4 candidate filtering (rejecting wildcard, loopback, link-local, multicast, broadcast, malformed, IPv6, hostname, and non-canonical values), opt-in/camera/address listener lifecycle decisions, address changes, and tokenized URL construction. Full verification: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain` succeeded; `git diff --check` succeeded. Authored diff is 365 lines (359 additions, 6 deletions), below the 400-line review guideline. Independent Huawei Wi-Fi smoke passed on Wi-Fi address `192.168.100.195` (`wlan0`): requests without a token and with a wrong token returned 401 for both index and stream; a malformed request returned 400; valid token requests returned index 200 and MJPEG 200 with the JPEG marker. After Wi-Fi was turned off and GreenCam stopped, the displayed URL was hidden and endpoint TCP probes timed out. No ADB forward was used. OBS was not rerun. No push, PR, release, or Engram write was performed. Work-unit commit: `2155f45` (`feat(camera): add opt-in Wi-Fi streaming`). T2a and T2b are complete; no further T2 implementation remains. Rollback boundary: revert the T2b listener/UI, Wi-Fi policy/tests, manifest permission, README, and this task-record update; the T2a authenticator and USB loopback behavior remain.

Release preparation: bumped Android metadata to versionCode `4` / versionName `0.1.0-alpha.4` and updated the README download link for the forthcoming alpha.4 debug APK. Verification: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain` succeeded; `aapt dump badging app/build/outputs/apk/debug/app-debug.apk` reported package `com.example.usbwebcam`, versionCode `4`, versionName `0.1.0-alpha.4`; `git diff --check` succeeded. No release was created or published.

## Relevant files
- `app/src/main/java/com/example/usbwebcam/MainActivity.java` — Android camera UI and lifecycle.
- `app/src/main/java/com/example/usbwebcam/MjpegRequestAuthenticator.java` — transport-independent per-session request-token validation.
- `app/src/main/java/com/example/usbwebcam/MjpegHttpServer.java` — optional protected HTTP request parsing; default remains unprotected.
- `app/src/test/java/com/example/usbwebcam/` — local unit tests.
- `README.md` — user-facing feature documentation.
