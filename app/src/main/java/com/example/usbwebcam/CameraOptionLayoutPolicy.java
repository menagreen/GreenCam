package com.example.usbwebcam;

final class CameraOptionLayoutPolicy {
    private CameraOptionLayoutPolicy() {}

    static int width(boolean vertical) {
        return vertical ? -1 : 0;
    }

    static float weight(boolean vertical) {
        return vertical ? 0f : 1f;
    }
}
