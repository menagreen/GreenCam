package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CameraOptionLayoutPolicyTest {
    @Test
    public void verticalOptionsFillWidthWithoutWeight() {
        assertEquals(-1, CameraOptionLayoutPolicy.width(true));
        assertEquals(0f, CameraOptionLayoutPolicy.weight(true), 0f);
    }

    @Test
    public void horizontalOptionsShareWidthByWeight() {
        assertEquals(0, CameraOptionLayoutPolicy.width(false));
        assertEquals(1f, CameraOptionLayoutPolicy.weight(false), 0f);
    }
}
