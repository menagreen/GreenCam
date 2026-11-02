# Wired USB and usability

## Objective
Make GreenCam usable over a USB data cable without ADB, then improve OBS handoff, latency, and navigation without changing it into a native USB Video Class device.

## Constraints
- U1 uses Android USB tethering as a local IP network. The user must enable tethering in system settings; a normal app cannot promise to enable it or to expose a UVC camera.
- Bind only to a detected concrete private IPv4 address on a USB tether interface. Keep the loopback ADB and opt-in Wi-Fi listeners unchanged. Use per-session bearer authentication, phone-approved pairing, and revoke on loss, IP change, or stop. No new privileged permission or background service; Android API 21 minimum.
- ADB may be used for development verification but must not be required by the shipped wired workflow.
- The Windows client remains a portable self-contained executable with no separately installed runtime. Tokenized URLs enter the clipboard only on explicit request.
- English technical artifacts and UI baseline; neutral Spanish Android UI resources.
- Engram mirror `odd/wired-usb-and-usability/tasks` is pending: no authoritative runtime session identity is available.

## Tasks and acceptance
- [ ] U1 — Opt-in USB-tether transport: discover compatible tether interface and private IPv4 without a fixed address; start protected MJPEG and phone-approved pairing on that address; close and revoke on address change, tether loss, camera stop, or explicit disable. Preserve Wi-Fi and ADB loopback. Checks: RED/GREEN policy tests, Android full test/build/lint, device tether smoke when manually enabled.
- [ ] U2 — Windows Copy OBS URL: enable the action only after pairing, copy the tokenized OBS page URL solely on explicit click, keep it out of ordinary labels/logs, and clear availability on disconnect. Allow at most two simultaneous authenticated MJPEG clients per listener so desktop preview and OBS can coexist; close all clients on listener stop. Checks: Android socket tests for two streams, cap and cleanup; desktop URL/policy tests; full Android and desktop checks; simultaneous runtime smoke; clipboard exposure warning.
- [ ] U3 — Measure and reduce glass-to-glass latency, focusing on stale-frame consumption and encode cost rather than guessing; report baseline and after measurement, with tests for bounded queues and no frame buildup.
- [ ] U4 — Reorganize Android and Windows interfaces into clear connection, camera/preview, and settings areas without losing existing controls or accessibility; device/desktop visual smoke.

## Route and delivery
U1 route: delegated direct (network policy, Activity/server lifecycle, UI/locale, tests and docs). Strict TDD enabled by project instruction; runner `gradlew.bat testDebugUnitTest`; full command `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain`; `git diff --check`. The feature forecast exceeds 400 authored changed lines; deliver sequential stacked-to-main PRs, one coherent work unit per slice. U1 itself is forecast under 400 lines. RDD is globally off. No device settings are changed without user action.

## Progress
U1 implementation complete, runtime validation pending. Strict TDD RED was observed twice: `testDebugUnitTest` failed compilation before `UsbTetherPolicy` and later `needsRestart` existed. GREEN/full `testDebugUnitTest assembleDebug lintDebug --offline --console=plain` passed (48 tasks); `git diff --check` passed. The Activity polls known Android tether interfaces while opted in and foregrounded, binds separate protected MJPEG/pairing sockets to a concrete private IPv4, rotates credentials on address change, and closes them on loss/stop. Existing Wi-Fi and loopback listeners remain distinct. On Huawei VOG-L04, `dumpsys connectivity` advertises tetherable `rndis0`, but `sys.usb.state` remained `hisuite,mtp,mass_storage,adb` and `rndis0` did not exist; the user has not enabled USB tethering, so no end-to-end wired smoke or OBS proof is claimed. Rollback U1 by reverting its policy, Activity wired listener/UI lifecycle, locale strings, tests, and README wired instructions together. Next: manually enable USB tethering, then verify binding, pairing, MJPEG and revocation without ADB forwarding.

U1 work-unit commit: `91c3820` (`feat(usb): add authenticated tether transport`), 381 authored changed lines. Runtime/device check is pending rather than marked complete.

U2 route: delegated direct because stream concurrency, desktop UI/core, tests, and README are non-trivial files. Strict TDD uses `gradlew.bat testDebugUnitTest` for Android and `dotnet run --project desktop/GreenCam.Core.Tests/GreenCam.Core.Tests.csproj` for desktop. The existing single-stream limit is a known blocker for simultaneous OBS and desktop preview. No background token display or implicit clipboard writes are authorized. Runtime smoke depends on a phone/OBS session and will be reported separately from unit checks.

U2 implementation: the Windows Copy OBS URL button is enabled only while a paired session is active and writes the tokenized OBS page URL only on explicit click. The ordinary UI never displays the token; the clipboard warning states that the secret can remain after disconnect. Each MJPEG listener now permits at most two stream clients, enough for desktop preview and OBS, and closes both when stopped. RED: the Android socket test expected a second simultaneous stream and failed under the old single-stream server; the desktop core test failed compilation before `ObsUrlPolicy` existed. GREEN/full Android `testDebugUnitTest assembleDebug lintDebug --offline --console=plain`, desktop core tests, self-contained win-x64 publish, and `git diff --check` passed. Runtime simultaneous desktop/OBS smoke and clipboard inspection were not performed in this work unit; U2 stays open until observed. Rollback: revert the two-client server/test and Windows URL policy/button/docs together.

U2 work-unit commit: `88b2df3` (`feat(obs): copy paired URL and allow two viewers`), 108 authored changed lines. A resumed writer repeated Android full checks, desktop core tests, self-contained win-x64 publish, and `git diff --check`; all passed. Runtime OBS/clipboard checks remain pending.
