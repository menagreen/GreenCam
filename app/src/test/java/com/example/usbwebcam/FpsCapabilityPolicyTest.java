package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FpsCapabilityPolicyTest {
    @Test
    public void maximumIsTakenAcrossAdvertisedRanges() {
        assertEquals(60, FpsCapabilityPolicy.advertisedMaximum(new int[] {30, 30, 30, 60}));
        assertEquals(30, FpsCapabilityPolicy.advertisedMaximum(new int[] {15, 30, 24, 30}));
    }

    @Test
    public void emptyOrMissingRangesStayUnknown() {
        assertEquals(FpsCapabilityPolicy.UNKNOWN_MAX_FPS, FpsCapabilityPolicy.advertisedMaximum(null));
        assertEquals(FpsCapabilityPolicy.UNKNOWN_MAX_FPS,
                FpsCapabilityPolicy.advertisedMaximum(new int[0]));
    }

    @Test
    public void knownMaximumDisablesCapsAboveItButUnknownKeepsExistingOptions() {
        assertTrue(FpsCapabilityPolicy.isOptionEnabled(15, 30));
        assertFalse(FpsCapabilityPolicy.isOptionEnabled(60, 30));
        assertTrue(FpsCapabilityPolicy.isOptionEnabled(60, FpsCapabilityPolicy.UNKNOWN_MAX_FPS));
    }

    @Test
    public void selectionFallsBackToHighestSupportedOptionWhenCameraChanges() {
        assertEquals(30, FpsCapabilityPolicy.reconcileSelection(60, 30));
        assertEquals(60, FpsCapabilityPolicy.reconcileSelection(60, 60));
        assertEquals(60, FpsCapabilityPolicy.reconcileSelection(
                60, FpsCapabilityPolicy.UNKNOWN_MAX_FPS));
        assertEquals(10, FpsCapabilityPolicy.reconcileSelection(15, 10));
    }
}
