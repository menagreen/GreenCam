package com.example.usbwebcam;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Per-server-session token protection for the small MJPEG HTTP request target. */
final class MjpegRequestAuthenticator {
    private static final int TOKEN_BYTES = 16;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final String token;

    static MjpegRequestAuthenticator create() {
        byte[] random = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(random);
        char[] encoded = new char[random.length * 2];
        for (int index = 0; index < random.length; index++) {
            int value = random[index] & 0xff;
            encoded[index * 2] = HEX[value >>> 4];
            encoded[index * 2 + 1] = HEX[value & 0x0f];
        }
        return new MjpegRequestAuthenticator(new String(encoded));
    }

    MjpegRequestAuthenticator(String token) {
        if (token == null || !token.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("Token must contain 128 bits encoded as lowercase hex");
        }
        this.token = token;
    }

    String tokenForSession() {
        return token;
    }

    boolean authorize(String requestTarget) {
        ParseResult parsed = parseProtectedTarget(requestTarget, token);
        return !parsed.malformed && parsed.authorized;
    }

    static ParseResult parseProtectedTarget(String requestTarget, String expectedToken) {
        if (requestTarget == null || requestTarget.isEmpty() || requestTarget.charAt(0) != '/'
                || requestTarget.indexOf('#') >= 0) {
            return ParseResult.malformed();
        }
        int queryStart = requestTarget.indexOf('?');
        if (queryStart < 0) {
            return new ParseResult(requestTarget, false, false);
        }
        if (queryStart != requestTarget.lastIndexOf('?')) {
            return ParseResult.malformed();
        }
        String path = requestTarget.substring(0, queryStart);
        String query = requestTarget.substring(queryStart + 1);
        if (path.isEmpty() || query.isEmpty() || query.indexOf('&') >= 0
                || query.indexOf('%') >= 0 || query.indexOf('+') >= 0) {
            return ParseResult.malformed();
        }
        int equals = query.indexOf('=');
        if (equals <= 0 || equals != query.lastIndexOf('=')) {
            return ParseResult.malformed();
        }
        String name = query.substring(0, equals);
        String providedToken = query.substring(equals + 1);
        if (!"token".equals(name) || providedToken.length() != 32
                || !providedToken.matches("[0-9a-f]{32}") || expectedToken == null
                || !expectedToken.matches("[0-9a-f]{32}")) {
            return ParseResult.malformed();
        }
        byte[] expected = expectedToken.getBytes(StandardCharsets.US_ASCII);
        byte[] provided = providedToken.getBytes(StandardCharsets.US_ASCII);
        return new ParseResult(path, false, MessageDigest.isEqual(expected, provided));
    }

    static final class ParseResult {
        final String path;
        final boolean malformed;
        final boolean authorized;

        private ParseResult(String path, boolean malformed, boolean authorized) {
            this.path = path;
            this.malformed = malformed;
            this.authorized = authorized;
        }

        static ParseResult malformed() {
            return new ParseResult(null, true, false);
        }
    }
}
