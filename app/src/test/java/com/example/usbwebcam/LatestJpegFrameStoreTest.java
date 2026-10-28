package com.example.usbwebcam;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LatestJpegFrameStoreTest {
    @Test
    public void onlyLatestFrameIsRetainedAndSessionSequenceRemainsMonotonic() throws Exception {
        LatestJpegFrameStore store = new LatestJpegFrameStore();
        long firstSession = store.beginSession();
        assertTrue(store.publish(firstSession, new byte[] {1}));
        LatestJpegFrameStore.Frame firstFrame = store.awaitAfter(-1, 1);
        assertNotNull(firstFrame);

        long secondSession = store.beginSession();
        assertFalse(store.publish(firstSession, new byte[] {2}));
        assertTrue(store.publish(secondSession, new byte[] {3}));
        LatestJpegFrameStore.Frame nextFrame = store.awaitAfter(firstFrame.sequence, 1);

        assertNotNull(nextFrame);
        assertTrue(nextFrame.sequence > firstFrame.sequence);
        assertArrayEquals(new byte[] {3}, nextFrame.jpeg);
    }

    @Test
    public void rejectsEmptyAndOversizedFramesAndClearsOnSessionEnd() throws Exception {
        LatestJpegFrameStore store = new LatestJpegFrameStore();
        long session = store.beginSession();
        assertFalse(store.publish(session, new byte[0]));
        assertFalse(store.publish(session, new byte[LatestJpegFrameStore.MAX_JPEG_BYTES + 1]));
        assertTrue(store.publish(session, new byte[] {4}));
        LatestJpegFrameStore.Frame frame = store.awaitAfter(-1, 1);
        assertNotNull(frame);

        store.endSession(session);

        assertNull(store.awaitAfter(frame.sequence, 1));
    }
}
