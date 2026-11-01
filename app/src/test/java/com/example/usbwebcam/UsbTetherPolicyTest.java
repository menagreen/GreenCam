package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.junit.Test;

public class UsbTetherPolicyTest {
    @Test public void selectsOnlyPrivateAddressesOnKnownTetherInterfaces() {
        assertEquals("192.168.42.129", UsbTetherPolicy.chooseAddress(Arrays.asList(
                new UsbTetherPolicy.Candidate("wlan0", "192.168.1.4"),
                new UsbTetherPolicy.Candidate("rndis0", "192.168.42.129"))));
        assertEquals("172.20.1.1", UsbTetherPolicy.chooseAddress(Arrays.asList(
                new UsbTetherPolicy.Candidate("usb0", "8.8.8.8"),
                new UsbTetherPolicy.Candidate("ncm0", "172.20.1.1"))));
    }

    @Test public void rejectsNonTetherOrUnsafeAddresses() {
        assertNull(UsbTetherPolicy.chooseAddress(Arrays.asList(
                new UsbTetherPolicy.Candidate("wlan0", "192.168.1.4"),
                new UsbTetherPolicy.Candidate("rndis0", "169.254.1.1"),
                new UsbTetherPolicy.Candidate("usb0", "127.0.0.1"),
                new UsbTetherPolicy.Candidate("ncm0", "192.168.01.1"))));
    }

    @Test public void listenerRequiresCameraOptInAndAddress() {
        assertEquals(true, UsbTetherPolicy.shouldListen(true, true, "10.0.0.1"));
        assertEquals(false, UsbTetherPolicy.shouldListen(false, true, "10.0.0.1"));
        assertEquals(false, UsbTetherPolicy.shouldListen(true, false, "10.0.0.1"));
        assertEquals(false, UsbTetherPolicy.shouldListen(true, true, null));
    }

    @Test public void addressChangeOrStoppedSocketRequiresFreshListeners() {
        assertEquals(false, UsbTetherPolicy.needsRestart("192.168.42.129",
                "192.168.42.129", true, true));
        assertEquals(true, UsbTetherPolicy.needsRestart("192.168.42.129",
                "192.168.44.1", true, true));
        assertEquals(true, UsbTetherPolicy.needsRestart("192.168.42.129",
                "192.168.42.129", true, false));
    }
}
