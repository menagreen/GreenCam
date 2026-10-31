package com.example.usbwebcam;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class PairingGateTest {
    @Test public void approvalIsRequiredAndCodeIsSingleUse() {
        AtomicLong now = new AtomicLong(1000);
        PairingGate gate = new PairingGate(now::get);
        String code = gate.begin("0123456789abcdef0123456789abcdef");
        assertTrue(code.matches("[0-9]{6}"));
        assertFalse(gate.submit("wrong", "192.168.1.2"));
        assertTrue(gate.submit(code, "192.168.1.2"));
        assertEquals("192.168.1.2", gate.pendingClient());
        assertTrue(gate.approve());
        assertEquals("0123456789abcdef0123456789abcdef", gate.awaitDecision(10));
        assertFalse(gate.submit(code, "192.168.1.2"));
    }

    @Test public void attemptsExpiryAndRevocationClosePairing() {
        AtomicLong now = new AtomicLong(1000);
        PairingGate gate = new PairingGate(now::get);
        String code = gate.begin("0123456789abcdef0123456789abcdef");
        for (int i = 0; i < 5; i++) assertFalse(gate.submit("wrong", "peer"));
        assertFalse(gate.submit(code, "peer"));
        code = gate.begin("0123456789abcdef0123456789abcdef");
        now.set(121001);
        assertFalse(gate.submit(code, "peer"));
        code = gate.begin("0123456789abcdef0123456789abcdef");
        assertTrue(gate.submit(code, "peer"));
        gate.revoke();
        assertFalse(gate.approve());
        assertNull(gate.awaitDecision(10));
    }
}
