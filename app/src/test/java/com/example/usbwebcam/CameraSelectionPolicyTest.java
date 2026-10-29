package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;

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
}
