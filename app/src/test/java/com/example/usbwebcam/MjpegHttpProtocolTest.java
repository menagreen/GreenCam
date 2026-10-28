package com.example.usbwebcam;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class MjpegHttpProtocolTest {
    @Test
    public void routesOnlyRootAndMjpegStreamGetRequests() {
        assertEquals(MjpegHttpProtocol.Route.INDEX,
                MjpegHttpProtocol.route("GET", "/"));
        assertEquals(MjpegHttpProtocol.Route.STREAM,
                MjpegHttpProtocol.route("GET", "/stream.mjpg"));
        assertEquals(MjpegHttpProtocol.Route.METHOD_NOT_ALLOWED,
                MjpegHttpProtocol.route("POST", "/stream.mjpg"));
        assertEquals(MjpegHttpProtocol.Route.NOT_FOUND,
                MjpegHttpProtocol.route("GET", "/private"));
    }

    @Test
    public void indexUsesRelativeStreamPathAndContainsNoDeviceAddress() {
        String page = MjpegHttpProtocol.indexPage();

        assertTrue(page.contains("src=\"/stream.mjpg\""));
        assertTrue(page.contains("USB Webcam"));
        assertTrue(!page.contains("192.168."));
    }

    @Test
    public void framePartContainsExactJpegBytesAndMultipartBoundary() {
        byte[] jpeg = new byte[] {(byte) 0xff, (byte) 0xd8, 0x12, (byte) 0xff, (byte) 0xd9};
        byte[] expectedHeader = ("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: 5"
                + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] expected = new byte[expectedHeader.length + jpeg.length + 2];
        System.arraycopy(expectedHeader, 0, expected, 0, expectedHeader.length);
        System.arraycopy(jpeg, 0, expected, expectedHeader.length, jpeg.length);
        expected[expected.length - 2] = '\r';
        expected[expected.length - 1] = '\n';

        assertArrayEquals(expected, MjpegHttpProtocol.framePart(jpeg));
    }

    @Test
    public void streamHeadersDeclareMultipartContentAndFixedBoundary() {
        String headers = new String(MjpegHttpProtocol.streamHeaders(), StandardCharsets.US_ASCII);

        assertTrue(headers.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(headers.contains("Content-Type: multipart/x-mixed-replace; boundary=frame\r\n"));
        assertTrue(headers.endsWith("\r\n\r\n"));
    }
}
