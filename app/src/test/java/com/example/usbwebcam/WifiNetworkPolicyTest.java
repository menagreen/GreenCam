package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class WifiNetworkPolicyTest {
    @Test
    public void choosesOnlyValidConcreteIpv4Candidates() {
        assertEquals("192.168.1.24", WifiNetworkPolicy.chooseAddress(Arrays.asList(
                "0.0.0.0", "127.0.0.1", "169.254.1.2", "224.0.0.1", "192.168.1.24")));
    }

    @Test
    public void rejectsNonIpv4MalformedAndNonConcreteCandidates() {
        assertNull(WifiNetworkPolicy.chooseAddress(Arrays.asList(
                "0.0.0.0", "127.0.0.1", "169.254.1.2", "255.255.255.255", "192.168.1.999",
                "192.168.1", "010.0.0.1", "2001:db8::1", "example.com")));
        assertNull(WifiNetworkPolicy.chooseAddress(Collections.emptyList()));
        assertNull(WifiNetworkPolicy.chooseAddress(null));
    }

    @Test
    public void listenerRunsOnlyWhenOptedInCameraRunningAndAddressAvailable() {
        assertTrue(WifiNetworkPolicy.shouldListen(true, true, "192.168.1.24"));
        assertFalse(WifiNetworkPolicy.shouldListen(false, true, "192.168.1.24"));
        assertFalse(WifiNetworkPolicy.shouldListen(true, false, "192.168.1.24"));
        assertFalse(WifiNetworkPolicy.shouldListen(true, true, null));
        assertFalse(WifiNetworkPolicy.shouldListen(true, true, "0.0.0.0"));
    }

    @Test
    public void listenerLifecycleStopsWithoutOptInOrCameraAndTracksAddressChanges() {
        assertEquals("192.168.1.24", WifiNetworkPolicy.desiredListenerAddress(true, true,
                Collections.singletonList("192.168.1.24")));
        assertEquals("192.168.1.25", WifiNetworkPolicy.desiredListenerAddress(true, true,
                Collections.singletonList("192.168.1.25")));
        assertNull(WifiNetworkPolicy.desiredListenerAddress(false, true,
                Collections.singletonList("192.168.1.24")));
        assertNull(WifiNetworkPolicy.desiredListenerAddress(true, false,
                Collections.singletonList("192.168.1.24")));
        assertNull(WifiNetworkPolicy.desiredListenerAddress(true, true,
                Collections.singletonList("0.0.0.0")));
    }

    @Test
    public void buildsTokenProtectedUrlForConcreteAddress() {
        assertEquals("http://192.168.1.24:8765/?token=0123456789abcdef0123456789abcdef",
                WifiNetworkPolicy.obsUrl("192.168.1.24", 8765,
                        "0123456789abcdef0123456789abcdef"));
        assertNull(WifiNetworkPolicy.obsUrl(null, 8765, "0123456789abcdef0123456789abcdef"));
        assertNull(WifiNetworkPolicy.obsUrl("0.0.0.0", 8765, "0123456789abcdef0123456789abcdef"));
    }

    @Test
    public void copyActionRequiresActiveProtectedWifiListener() {
        assertTrue(WifiNetworkPolicy.canCopyObsUrl(true, true, true,
                "192.168.1.24", true));
        assertFalse(WifiNetworkPolicy.canCopyObsUrl(false, true, true,
                "192.168.1.24", true));
        assertFalse(WifiNetworkPolicy.canCopyObsUrl(true, false, true,
                "192.168.1.24", true));
        assertFalse(WifiNetworkPolicy.canCopyObsUrl(true, true, false,
                "192.168.1.24", true));
        assertFalse(WifiNetworkPolicy.canCopyObsUrl(true, true, true,
                "0.0.0.0", true));
        assertFalse(WifiNetworkPolicy.canCopyObsUrl(true, true, true,
                "192.168.1.24", false));
    }
}
