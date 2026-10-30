package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ScreenDimPolicyTest {
    @Test
    public void dimBrightnessIsAtMinimum() {
        assertEquals(0.0f, ScreenDimPolicy.minimumBrightness(), 0.0f);
    }

    @Test
    public void originalBrightnessIsRestoredIncludingSystemDefault() {
        assertEquals(0.65f, ScreenDimPolicy.brightnessToRestore(0.65f), 0.0f);
        assertEquals(-1.0f, ScreenDimPolicy.brightnessToRestore(-1.0f), 0.0f);
    }
}
