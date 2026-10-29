# GreenCam

GreenCam turns an Android phone into a video-only webcam for OBS Studio over a USB cable. The Android app serves a live MJPEG stream on device loopback; ADB forwards that stream to an OBS Browser Source on Windows. It does not use DroidCam, vendor-specific APIs, Wi-Fi streaming, audio capture, or background camera service.

## Requirements

- Android 5.0 (API 21) or newer with a working CameraX-compatible camera.
- A Windows PC with Android Platform Tools (ADB), OBS Studio, and a USB data cable.
- USB debugging enabled and authorized for that PC while forwarding is in use.

## Quick setup

### 1. Install and start GreenCam

Download the [v0.1.0-alpha.2 preview APK](https://github.com/menagreen/GreenCam/releases/download/v0.1.0-alpha.2/GreenCam-v0.1.0-alpha.2-debug.apk) or build the debug APK from source below, then install it on the phone. This preview is debug-signed, not a production-signed release; a future build with a different signing key may require uninstalling it first. Open **GreenCam**, choose the available **Rear camera** or **Front camera**, select an output limit of **10**, **15**, **30**, or **60 FPS**, then tap **Start camera** and grant camera permission when Android asks. Keep GreenCam in the foreground; leaving the app stops the camera and stream.

### 2. Forward the stream over USB

Connect and unlock the phone, approve the USB debugging prompt, and confirm ADB sees it:

```powershell
adb devices
```

The device should appear with status `device`, not `unauthorized` or `offline`. Then create the USB port forward:

```powershell
adb forward tcp:8765 tcp:8765
```

The forwarding command is for a USB-connected device; do not replace it with a network or reverse-tunnel command. GreenCam's Android listener is bound only to `127.0.0.1:8765` on the phone.

### 3. Add the camera to OBS

1. In OBS, add a **Browser** source to a scene.
2. Leave **Local file** off and set **URL** to `http://localhost:8765/`.
3. Set the browser viewport to **1280 × 720**. The source page loads the MJPEG stream from `/stream.mjpg`.
4. Confirm the phone image appears, then select **Start Virtual Camera** in OBS.
5. Choose **OBS Virtual Camera** in the video-call or capture application.

See the [OBS Browser Source guide](https://obsproject.com/kb/browser-source) for the source properties.

## Controls and privacy

- **Start camera / Stop camera** controls preview and streaming together.
- **Rear camera / Front camera** shows the active choice; unavailable cameras cannot be selected. You can change cameras while running.
- **Output rate (up to)** limits the MJPEG output to 10, 15, 30, or 60 FPS. The default is 15 FPS. GreenCam displays the selected camera's advertised Camera2 capture maximum and disables higher output caps when that metadata is available; unsupported or unavailable metadata is shown as **Unknown**, with the existing caps left enabled. This advertised maximum is not a measured or guaranteed stream rate: actual MJPEG throughput can be lower because of camera hardware, device load, or encoding. The output cap controls pacing only; it does not request a camera capture mode or create/interpolate frames.
- The app requests camera permission at runtime; the `INTERNET` permission is a normal install-time permission used for the local socket. It captures video only.
- The HTTP server listens on phone loopback only, not on Wi-Fi or cellular interfaces. ADB forwarding provides the USB bridge to the host.
- Streaming ends when you stop the camera or leave GreenCam. No background service or cloud upload is used.
- When finished, stop the camera in GreenCam and remove the host forward:

  ```powershell
  adb forward --remove tcp:8765
  ```

- Disable USB debugging when you no longer need it. USB debugging authorizes the connected computer to use ADB; keep the phone unlocked only while setting up the connection.

## Build from source

The project uses Java 11 source compatibility, Gradle 8.9, Android Gradle Plugin 8.7.3, Android SDK Platform 35, and Build Tools 35.0.0. Use JDK 17. The Gradle wrapper downloads Gradle on first use; install Android SDK components with the official Android SDK Manager and accept their licenses. Set `ANDROID_HOME` to the SDK directory (or create the ignored `local.properties` with `sdk.dir=<SDK path>`).

From the repository root in PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Troubleshooting

| Symptom | Check |
|---|---|
| ADB reports `unauthorized` or no device | Unlock the phone, approve the USB debugging prompt, use a data-capable cable, and run `adb devices` again. |
| OBS source is blank | Make sure GreenCam is foregrounded and says the camera/USB stream is running; verify `adb forward --list` includes port 8765; open `http://localhost:8765/` on the PC and refresh the Browser source. |
| GreenCam cannot start the local stream | Another process may already be using port 8765 on the phone. Stop GreenCam, close the other camera/stream app, then try again. |
| Stream stops after switching apps | This is intentional foreground-only behavior. Return to GreenCam and tap **Start camera** again. |
| Virtual Camera is unavailable | Confirm the Browser source displays video, then use OBS **Start Virtual Camera** and select **OBS Virtual Camera** in the other app. |

Remove the port forward after use with `adb forward --remove tcp:8765`. The project license is in [LICENSE](LICENSE).
