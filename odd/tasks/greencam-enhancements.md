# GreenCam Enhancements

## Objective
Deliver the requested GreenCam backlog in reviewable work units while preserving Android API 21 compatibility and avoiding new permissions/services unless separately authorized.

## Problem and why
The user requested multiple camera and usability improvements. The first selected work unit is a user-reversible black, minimum-brightness screen mode that leaves the app foregrounded and camera/stream active. This is explicitly not physical display-off or background-service behavior.

## Scope and constraints
- T1: add black screen mode with minimum window brightness; keep camera/stream active; accessible user exit; restore brightness on exit, `onStop`, and destroy; prevent bright static controls/burn-in; update concise README guidance.
- Later, pending: Wi-Fi; ADB-free transport; Windows desktop companion/filters; physical rear lenses; zoom/focus; dark mode; menu/aesthetic UI; multilingual support.
- No new permissions/services; no physical display-off; Android API 21 minimum; English artifact language.
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
- [ ] T2 — Wi-Fi enhancements. Pending user backlog; not scoped or implemented.
- [ ] T3 — ADB-free transport. Pending user backlog; not scoped or implemented.
- [ ] T4 — Windows desktop companion/filters. Pending user backlog; not scoped or implemented.
- [ ] T5 — Physical rear lenses. Pending user backlog; not scoped or implemented.
- [ ] T6 — Zoom/focus. Pending user backlog; not scoped or implemented.
- [ ] T7 — Dark mode. Pending user backlog; not scoped or implemented.
- [ ] T8 — Menu/aesthetic UI. Pending user backlog; not scoped or implemented.
- [ ] T9 — Multilingual support. Pending user backlog; not scoped or implemented.

## Progress and next step
T1 implementation complete. Verification: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain` succeeded; `git diff --check` succeeded. Runtime/device harness: N/A — no authorized device interaction was requested, and this work only changes activity UI/window policy. Rollback boundary: revert the single T1 commit to remove the dim UI, brightness handling, policy tests, and documentation together. Commit identity: `ecb69c7` (`feat(camera): add reversible dim screen mode`). Engram mirror pending (no authoritative session identity available and parent requested no Engram writes).

## Relevant files
- `app/src/main/java/com/example/usbwebcam/MainActivity.java` — Android camera UI and lifecycle.
- `app/src/test/java/com/example/usbwebcam/` — local unit tests.
- `README.md` — user-facing feature documentation.
