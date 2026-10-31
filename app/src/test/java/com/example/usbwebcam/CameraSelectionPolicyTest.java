package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

public class CameraSelectionPolicyTest {
    @Test
    public void switchCameraAlternatesBetweenFrontAndBack() {
        assertEquals(CameraSelectionPolicy.LENS_FRONT,
                CameraSelectionPolicy.switchLens(CameraSelectionPolicy.LENS_BACK));
        assertEquals(CameraSelectionPolicy.LENS_BACK,
                CameraSelectionPolicy.switchLens(CameraSelectionPolicy.LENS_FRONT));
    }

    @Test
    public void unknownLensFallsBackToBackCamera() {
        assertEquals(CameraSelectionPolicy.LENS_BACK,
                CameraSelectionPolicy.switchLens(-1));
    }

    @Test
    public void explicitSelectionKeepsCurrentLensWhenRequestedCameraIsUnavailable() {
        assertEquals(CameraSelectionPolicy.LENS_BACK,
                CameraSelectionPolicy.selectLens(CameraSelectionPolicy.LENS_BACK,
                        CameraSelectionPolicy.LENS_FRONT, false));
        assertEquals(CameraSelectionPolicy.LENS_FRONT,
                CameraSelectionPolicy.selectLens(CameraSelectionPolicy.LENS_BACK,
                        CameraSelectionPolicy.LENS_FRONT, true));
    }

    @Test
    public void defaultSelectionFallsBackToAvailableFrontCamera() {
        assertEquals(CameraSelectionPolicy.LENS_FRONT,
                CameraSelectionPolicy.defaultLens(false, true));
        assertEquals(CameraSelectionPolicy.LENS_BACK,
                CameraSelectionPolicy.defaultLens(true, true));
    }

    @Test
    public void rearIdSelectionRejectsIdsNoLongerExposed() {
        assertEquals("2", CameraSelectionPolicy.selectRearId("0", "2", Arrays.asList("0", "2")));
        assertEquals("0", CameraSelectionPolicy.selectRearId("0", "missing", Arrays.asList("0", "2")));
        assertEquals("2", CameraSelectionPolicy.selectRearId("missing", "missing", Arrays.asList("2")));
        assertNull(CameraSelectionPolicy.selectRearId(null, "missing", Arrays.asList()));
    }
}
