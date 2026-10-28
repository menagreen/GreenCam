package com.example.usbwebcam;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PreviewStartGateTest {
    @Test
    public void pendingStartWaitsForProviderAndPermissionThenStartsOnce() {
        PreviewStartGate gate = new PreviewStartGate();
        gate.requestStart();

        assertFalse(gate.consumeIfReady(false, true));
        assertFalse(gate.consumeIfReady(true, false));
        assertTrue(gate.consumeIfReady(true, true));
        assertFalse(gate.consumeIfReady(true, true));
    }

    @Test
    public void cancellationPreventsLateProviderCompletionFromStartingPreview() {
        PreviewStartGate gate = new PreviewStartGate();
        gate.requestStart();
        gate.cancel();

        assertFalse(gate.consumeIfReady(true, true));
    }
}
