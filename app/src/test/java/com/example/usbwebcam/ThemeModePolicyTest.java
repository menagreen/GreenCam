package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ThemeModePolicyTest {
    @Test
    public void invalidStoredModeFallsBackToSystem() {
        assertEquals(ThemeModePolicy.SYSTEM, ThemeModePolicy.sanitize(-1));
        assertEquals(ThemeModePolicy.SYSTEM, ThemeModePolicy.sanitize(99));
    }

    @Test
    public void cyclesThroughAllModes() {
        assertEquals(ThemeModePolicy.LIGHT, ThemeModePolicy.next(ThemeModePolicy.SYSTEM));
        assertEquals(ThemeModePolicy.DARK, ThemeModePolicy.next(ThemeModePolicy.LIGHT));
        assertEquals(ThemeModePolicy.SYSTEM, ThemeModePolicy.next(ThemeModePolicy.DARK));
    }
}
