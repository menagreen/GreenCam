package com.example.usbwebcam;

import java.util.List;
import java.util.Locale;

/** Selects an Android USB-tether interface without assuming the OEM's IPv4 subnet. */
final class UsbTetherPolicy {
    static final class Candidate {
        final String interfaceName;
        final String address;

        Candidate(String interfaceName, String address) {
            this.interfaceName = interfaceName;
            this.address = address;
        }
    }

    private UsbTetherPolicy() { }

    static String chooseAddress(List<Candidate> candidates) {
        if (candidates == null) return null;
        for (Candidate candidate : candidates) {
            if (candidate != null && isTetherName(candidate.interfaceName)
                    && isPrivateIpv4(candidate.address)) return candidate.address;
        }
        return null;
    }

    static boolean shouldListen(boolean cameraRunning, boolean enabled, String address) {
        return cameraRunning && enabled && isPrivateIpv4(address);
    }

    static boolean needsRestart(String currentAddress, String selectedAddress,
                                boolean streamRunning, boolean pairingRunning) {
        return selectedAddress != null && (!selectedAddress.equals(currentAddress)
                || !streamRunning || !pairingRunning);
    }

    private static boolean isTetherName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.matches("(rndis|usb|ncm)[0-9]+(?:[._][0-9]+)*");
    }

    private static boolean isPrivateIpv4(String address) {
        if (address == null || !address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return false;
        String[] parts = address.split("\\.");
        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
            try {
                octets[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException exception) {
                return false;
            }
            if (octets[i] > 255 || !parts[i].equals(Integer.toString(octets[i]))) return false;
        }
        return octets[0] == 10 || octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31
                || octets[0] == 192 && octets[1] == 168;
    }
}
