package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ZoomControlPolicyTest {
    @Test public void mapsSliderToAdvertisedRangeAndClampsProgress() {
        assertEquals(1f, ZoomControlPolicy.ratioForProgress(0, 1f, 5f), 0.001f);
        assertEquals(3f, ZoomControlPolicy.ratioForProgress(50, 1f, 5f), 0.001f);
        assertEquals(5f, ZoomControlPolicy.ratioForProgress(100, 1f, 5f), 0.001f);
        assertEquals(1f, ZoomControlPolicy.ratioForProgress(-5, 1f, 5f), 0.001f);
        assertEquals(5f, ZoomControlPolicy.ratioForProgress(105, 1f, 5f), 0.001f);
    }

    @Test public void cameraSwitchResetsZoomWithinNewRange() {
        assertEquals(1f, ZoomControlPolicy.initialRatio(1f, 8f), 0.001f);
        assertEquals(2f, ZoomControlPolicy.initialRatio(2f, 8f), 0.001f);
        assertEquals(1f, ZoomControlPolicy.initialRatio(0.5f, 1f), 0.001f);
    }

    @Test public void rejectsInvalidOrFixedCapability() {
        assertTrue(ZoomControlPolicy.supportsZoom(1f, 3f));
        assertFalse(ZoomControlPolicy.supportsZoom(1f, 1f));
        assertFalse(ZoomControlPolicy.supportsZoom(Float.NaN, 3f));
    }
}
