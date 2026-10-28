package com.example.usbwebcam;

import androidx.camera.core.CameraSelector;

final class CameraSelectionPolicy {
    static final int LENS_BACK = CameraSelector.LENS_FACING_BACK;
    static final int LENS_FRONT = CameraSelector.LENS_FACING_FRONT;

    private CameraSelectionPolicy() { }

    static int switchLens(int lensFacing) {
        return lensFacing == LENS_BACK ? LENS_FRONT : LENS_BACK;
    }
}
