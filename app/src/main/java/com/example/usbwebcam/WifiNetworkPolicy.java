package com.example.usbwebcam;

import java.util.List;

/** Pure policy for selecting a concrete IPv4 address for the optional Wi-Fi listener. */
final class WifiNetworkPolicy {
    private WifiNetworkPolicy() { }

    static String chooseAddress(List<String> candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (isConcreteIpv4(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static boolean shouldListen(boolean cameraRunning, boolean enabled, String address) {
        return cameraRunning && enabled && isConcreteIpv4(address);
    }

    static boolean canCopyObsUrl(boolean cameraRunning, boolean enabled,
            boolean listenerRunning, String address, boolean tokenReady) {
        return shouldListen(cameraRunning, enabled, address) && listenerRunning && tokenReady;
    }

    static String desiredListenerAddress(boolean cameraRunning, boolean enabled,
            List<String> candidates) {
        return cameraRunning && enabled ? chooseAddress(candidates) : null;
    }

    static String obsUrl(String address, int port, String token) {
        if (!isConcreteIpv4(address) || port < 1 || port > 65535
                || token == null || !token.matches("[0-9a-f]{32}")) {
            return null;
        }
        return "http://" + address + ":" + port + "/?token=" + token;
    }

    private static boolean isConcreteIpv4(String address) {
        if (address == null || !address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            return false;
        }
        String[] parts = address.split("\\.");
        int[] octets = new int[4];
        for (int index = 0; index < parts.length; index++) {
            try {
                octets[index] = Integer.parseInt(parts[index]);
            } catch (NumberFormatException exception) {
                return false;
            }
            if (octets[index] > 255) {
                return false;
            }
            if (!parts[index].equals(Integer.toString(octets[index]))) {
                return false;
            }
        }
        return octets[0] != 0 && octets[0] != 127
                && !(octets[0] == 169 && octets[1] == 254)
                && octets[0] < 224
                && !(octets[0] == 255 && octets[1] == 255
                && octets[2] == 255 && octets[3] == 255);
    }
}
