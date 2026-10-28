package com.example.usbwebcam;

final class PreviewStartGate {
    private boolean startRequested;

    void requestStart() {
        startRequested = true;
    }

    boolean consumeIfReady(boolean providerReady, boolean permissionGranted) {
        if (!startRequested || !providerReady || !permissionGranted) {
            return false;
        }
        startRequested = false;
        return true;
    }

    void cancel() {
        startRequested = false;
    }
}
