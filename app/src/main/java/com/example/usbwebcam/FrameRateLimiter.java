package com.example.usbwebcam;

final class FrameRateLimiter {
    private int maxFps = 15;
    private long lastEncodedAtNanos = Long.MIN_VALUE;

    synchronized int getMaxFps() {
        return maxFps;
    }

    synchronized void setMaxFps(int fps) {
        if (fps != 10 && fps != 15 && fps != 30 && fps != 60) {
            throw new IllegalArgumentException("Unsupported FPS cap: " + fps);
        }
        maxFps = fps;
    }

    synchronized boolean shouldEncode(long nowNanos) {
        long intervalNanos = 1_000_000_000L / maxFps;
        if (lastEncodedAtNanos != Long.MIN_VALUE && nowNanos >= lastEncodedAtNanos
                && nowNanos - lastEncodedAtNanos < intervalNanos) {
            return false;
        }
        lastEncodedAtNanos = nowNanos;
        return true;
    }

    synchronized void reset() {
        lastEncodedAtNanos = Long.MIN_VALUE;
    }
}
