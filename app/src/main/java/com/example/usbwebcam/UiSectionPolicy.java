package com.example.usbwebcam;

/** Keeps navigation independent from the camera and transport lifecycles. */
final class UiSectionPolicy {
    static final int CAMERA = 0;
    static final int CONNECTION = 1;
    static final int SETTINGS = 2;

    private UiSectionPolicy() { }

    static int sanitize(int section) {
        return section >= CAMERA && section <= SETTINGS ? section : CAMERA;
    }

    static boolean isVisible(int selected, int section) {
        return sanitize(selected) == section;
    }
}
