# About GreenCam

## Objective
Give Android and Windows users a discoverable About view explaining who created GreenCam and whom it helps.

## Scope and constraints
- Android: a small About action/dialog with English and neutral Spanish copy following the device language.
- Windows: a simple About action/dialog in the portable WinForms app, with English copy.
- Copy states that GreenCam was created by menagreen for content creators who cannot afford a webcam or want to make use of their phone camera. The user-supplied Twitch link may be included.
- Preserve all camera, pairing, preview, and stream behavior. No new permissions, third-party dependencies, or remote features.
- Engram mirror `odd/about-menagreen/tasks` is pending because no authoritative runtime session identity is available.

## Acceptance and tasks
- [ ] A1 — Add discoverable Android About UI, localized strings, and focused verification.
- [ ] A2 — Add discoverable Windows About UI and focused verification.
- [ ] A3 — Run full Android and desktop checks; record observed results and commit a reviewable work unit.

## Route, tests, and delivery
- Route: delegated direct; both platform UIs and locale resources are non-trivial files, and CodeGraph mapped the relevant UI constructors.
- Strict TDD: enabled by project instruction; Android runner `gradlew.bat testDebugUnitTest`; desktop runner `dotnet run --project desktop/GreenCam.Core.Tests/GreenCam.Core.Tests.csproj`.
- Full checks: Android `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain`; desktop core tests and self-contained `dotnet publish`; `git diff --check`.
- Forecast: below 400 authored changed lines; delivery strategy remains sequential PRs to main.

## Progress
Android and Windows About actions/dialogs are implemented, with English and neutral Spanish Android resources. The Android action sits in the header; the Windows action sits beside Pair. No new permissions or packages were added. Strict TDD RED was observed: Android resource-copy test failed before `about_message` existed, and desktop core tests failed compilation before `AboutText` existed. GREEN/full Android `testDebugUnitTest assembleDebug lintDebug --offline --console=plain` passed (48 tasks), desktop core tests passed, self-contained single-file win-x64 publish passed, and `git diff --check` passed. Runtime UI smoke remains pending because this bounded writer did not alter the device or launch the desktop GUI. Rollback: revert the header and desktop About buttons/dialogs, their localized/core copy, tests, and README credit together. Next: independent UI smoke, then close A1–A3.
