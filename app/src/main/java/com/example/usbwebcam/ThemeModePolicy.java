package com.example.usbwebcam;

final class ThemeModePolicy {
    static final int SYSTEM = 0;
    static final int LIGHT = 1;
    static final int DARK = 2;

    private ThemeModePolicy() {}

    static int sanitize(int mode) {
        return mode >= SYSTEM && mode <= DARK ? mode : SYSTEM;
    }

    static int next(int mode) {
        return (sanitize(mode) + 1) % 3;
    }
}
