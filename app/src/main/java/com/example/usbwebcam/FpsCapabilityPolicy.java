package com.example.usbwebcam;

final class FpsCapabilityPolicy {
    static final int UNKNOWN_MAX_FPS = -1;
    private static final int[] OUTPUT_CAPS = {10, 15, 30, 60};

    private FpsCapabilityPolicy() { }

    static int advertisedMaximum(int[] rangeUpperBounds) {
        if (rangeUpperBounds == null || rangeUpperBounds.length == 0) {
            return UNKNOWN_MAX_FPS;
        }
        int maximum = UNKNOWN_MAX_FPS;
        for (int upperBound : rangeUpperBounds) {
            maximum = Math.max(maximum, upperBound);
        }
        return maximum;
    }

    static boolean isOptionEnabled(int outputCap, int advertisedMaximum) {
        return advertisedMaximum == UNKNOWN_MAX_FPS || outputCap <= advertisedMaximum;
    }

    static int reconcileSelection(int selectedCap, int advertisedMaximum) {
        if (advertisedMaximum == UNKNOWN_MAX_FPS || selectedCap <= advertisedMaximum) {
            return selectedCap;
        }
        int fallback = OUTPUT_CAPS[0];
        for (int outputCap : OUTPUT_CAPS) {
            if (outputCap <= advertisedMaximum) {
                fallback = outputCap;
            }
        }
        return fallback;
    }
}
