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
- [ ] U2 — Windows Copy OBS URL with explicit clipboard action and bounded multi-client stream support so preview and OBS can coexist. Checks: Android socket tests, desktop tests, simultaneous runtime smoke, no token in ordinary UI/logs.
- [ ] U3 — Measure and reduce glass-to-glass latency, focusing on stale-frame consumption and encode cost rather than guessing; report baseline and after measurement, with tests for bounded queues and no frame buildup.
- [ ] U4 — Reorganize Android and Windows interfaces into clear connection, camera/preview, and settings areas without losing existing controls or accessibility; device/desktop visual smoke.

## Route and delivery
U1 route: delegated direct (network policy, Activity/server lifecycle, UI/locale, tests and docs). Strict TDD enabled by project instruction; runner `gradlew.bat testDebugUnitTest`; full command `gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --console=plain`; `git diff --check`. The feature forecast exceeds 400 authored changed lines; deliver sequential stacked-to-main PRs, one coherent work unit per slice. U1 itself is forecast under 400 lines. RDD is globally off. No device settings are changed without user action.

## Progress
U1 implementation complete, runtime validation pending. Strict TDD RED was observed twice: `testDebugUnitTest` failed compilation before `UsbTetherPolicy` and later `needsRestart` existed. GREEN/full `testDebugUnitTest assembleDebug lintDebug --offline --console=plain` passed (48 tasks); `git diff --check` passed. The Activity polls known Android tether interfaces while opted in and foregrounded, binds separate protected MJPEG/pairing sockets to a concrete private IPv4, rotates credentials on address change, and closes them on loss/stop. Existing Wi-Fi and loopback listeners remain distinct. On Huawei VOG-L04, `dumpsys connectivity` advertises tetherable `rndis0`, but `sys.usb.state` remained `hisuite,mtp,mass_storage,adb` and `rndis0` did not exist; the user has not enabled USB tethering, so no end-to-end wired smoke or OBS proof is claimed. Rollback U1 by reverting its policy, Activity wired listener/UI lifecycle, locale strings, tests, and README wired instructions together. Next: manually enable USB tethering, then verify binding, pairing, MJPEG and revocation without ADB forwarding.

U1 work-unit commit: `91c3820` (`feat(usb): add authenticated tether transport`), 381 authored changed lines. Runtime/device check is pending rather than marked complete.
