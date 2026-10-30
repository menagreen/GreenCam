package com.example.usbwebcam;

final class ZoomControlPolicy {
    private ZoomControlPolicy() { }

    static boolean supportsZoom(float min, float max) {
        return finite(min) && finite(max) && min > 0f && max > min;
    }

    static float initialRatio(float min, float max) {
        if (!finite(min) || !finite(max) || min <= 0f || max < min) {
            return 1f;
        }
        return Math.max(min, Math.min(max, 1f));
    }

    static float ratioForProgress(int progress, float min, float max) {
        if (!supportsZoom(min, max)) {
            return initialRatio(min, max);
        }
        int clamped = Math.max(0, Math.min(100, progress));
        return min + (max - min) * clamped / 100f;
    }

    static int progressForRatio(float ratio, float min, float max) {
        if (!supportsZoom(min, max) || !finite(ratio)) {
            return 0;
        }
        return Math.max(0, Math.min(100, Math.round((ratio - min) * 100f / (max - min))));
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
