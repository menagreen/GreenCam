package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UiSectionPolicyTest {
    @Test public void defaultsToCameraAndSanitizesUnknownSelections() {
        assertEquals(UiSectionPolicy.CAMERA, UiSectionPolicy.sanitize(-1));
        assertEquals(UiSectionPolicy.CAMERA, UiSectionPolicy.sanitize(42));
        assertEquals(UiSectionPolicy.CONNECTION, UiSectionPolicy.sanitize(UiSectionPolicy.CONNECTION));
    }

    @Test public void onlySelectedSectionIsVisible() {
        assertTrue(UiSectionPolicy.isVisible(UiSectionPolicy.CONNECTION, UiSectionPolicy.CONNECTION));
        assertFalse(UiSectionPolicy.isVisible(UiSectionPolicy.CONNECTION, UiSectionPolicy.CAMERA));
        assertFalse(UiSectionPolicy.isVisible(UiSectionPolicy.CONNECTION, UiSectionPolicy.SETTINGS));
    }
}
