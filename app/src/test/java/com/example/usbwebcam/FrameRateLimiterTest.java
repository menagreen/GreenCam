package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FrameRateLimiterTest {
    @Test
    public void defaultsToFifteenFpsAndRejectsUnsupportedCaps() {
        FrameRateLimiter limiter = new FrameRateLimiter();
        assertEquals(15, limiter.getMaxFps());
        for (int fps : new int[] {10, 15, 30}) {
            limiter.setMaxFps(fps);
            assertEquals(fps, limiter.getMaxFps());
        }
        try {
            limiter.setMaxFps(60);
            throw new AssertionError("Unsupported FPS cap was accepted");
        } catch (IllegalArgumentException expected) {
            assertEquals(30, limiter.getMaxFps());
        }
    }

    @Test
    public void limitsFramesAndAppliesChangesWithoutReset() {
        FrameRateLimiter limiter = new FrameRateLimiter();
        assertTrue(limiter.shouldEncode(0));
        assertFalse(limiter.shouldEncode(66_000_000));
        assertTrue(limiter.shouldEncode(67_000_000));

        limiter.setMaxFps(30);
        assertFalse(limiter.shouldEncode(100_000_000));
        assertTrue(limiter.shouldEncode(101_000_000));

        limiter.setMaxFps(10);
        assertFalse(limiter.shouldEncode(200_000_000));
        assertTrue(limiter.shouldEncode(201_000_000));
    }

    @Test
    public void resetAllowsTheFirstFrameOfANewCameraSession() {
        FrameRateLimiter limiter = new FrameRateLimiter();
        assertTrue(limiter.shouldEncode(500_000_000));
        assertFalse(limiter.shouldEncode(500_000_001));
        limiter.reset();
        assertTrue(limiter.shouldEncode(500_000_001));
    }
}
