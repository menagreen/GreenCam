package com.example.usbwebcam;

final class ScreenDimPolicy {
    private ScreenDimPolicy() { }

    static float minimumBrightness() {
        return 0.0f;
    }

    static float brightnessToRestore(float originalBrightness) {
        return originalBrightness;
    }
}
