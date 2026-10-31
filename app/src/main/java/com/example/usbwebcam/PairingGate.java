package com.example.usbwebcam;

import java.security.SecureRandom;


/** One short-lived pairing invitation, requiring an explicit phone approval. */
final class PairingGate {
    private static final long LIFETIME_MS = 120_000;
    private static final int MAX_FAILURES = 5;
    interface Clock { long now(); }
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private String code;
    private String token;
    private String pendingClient;
    private long expiresAt;
    private int failures;
    private boolean approved;

    PairingGate(Clock clock) {
        this.clock = clock;
    }

    synchronized String begin(String sessionToken) {
        revoke();
        if (sessionToken == null || !sessionToken.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("A valid session token is required");
        }
        code = String.format(java.util.Locale.US, "%06d", random.nextInt(1_000_000));
        token = sessionToken;
        expiresAt = clock.now() + LIFETIME_MS;
        return code;
    }

    synchronized boolean submit(String candidate, String client) {
        if (code == null || pendingClient != null || clock.now() >= expiresAt) return false;
        if (++failures > MAX_FAILURES) {
            revoke();
            return false;
        }
        if (!code.equals(candidate)) {
            if (failures == MAX_FAILURES) revoke();
            return false;
        }
        pendingClient = client;
        notifyAll();
        return true;
    }

    synchronized String pendingClient() {
        return pendingClient;
    }

    synchronized boolean approve() {
        if (pendingClient == null || code == null || clock.now() >= expiresAt) return false;
        approved = true;
        notifyAll();
        return true;
    }

    synchronized String awaitDecision(long maxWaitMs) {
        long deadline = clock.now() + maxWaitMs;
        while (code != null && pendingClient != null && !approved && clock.now() < expiresAt) {
            long remaining = Math.min(expiresAt, deadline) - clock.now();
            if (remaining <= 0) break;
            try {
                wait(remaining);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        String result = approved && code != null && clock.now() < expiresAt ? token : null;
        revoke();
        return result;
    }

    synchronized void revoke() {
        code = null;
        token = null;
        pendingClient = null;
        approved = false;
        failures = 0;
        expiresAt = 0;
        notifyAll();
    }
}
