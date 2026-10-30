package com.example.usbwebcam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MjpegRequestAuthenticatorTest {
    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @Test
    public void generatesFreshUrlSafeTokenWithAtLeast128Bits() {
        MjpegRequestAuthenticator first = MjpegRequestAuthenticator.create();
        MjpegRequestAuthenticator second = MjpegRequestAuthenticator.create();

        assertEquals(32, first.tokenForSession().length());
        assertTrue(first.tokenForSession().matches("[0-9a-f]{32}"));
        assertNotEquals(first.tokenForSession(), second.tokenForSession());
    }

    @Test
    public void requiresExactTokenForProtectedRoutes() {
        MjpegRequestAuthenticator authenticator = new MjpegRequestAuthenticator(TOKEN);

        assertTrue(authenticator.authorize("/?token=" + TOKEN));
        assertTrue(authenticator.authorize("/stream.mjpg?token=" + TOKEN));
        assertFalse(authenticator.authorize("/?token=" + TOKEN.substring(1)));
        assertFalse(authenticator.authorize("/?token=" + TOKEN + "0"));
    }

    @Test
    public void rejectsMalformedAndDuplicateQueryParameters() {
        MjpegRequestAuthenticator authenticator = new MjpegRequestAuthenticator(TOKEN);

        assertFalse(authenticator.authorize("/?token=" + TOKEN + "&token=" + TOKEN));
        assertFalse(authenticator.authorize("/?other=x&token=" + TOKEN));
        assertFalse(authenticator.authorize("/?token"));
        assertFalse(authenticator.authorize("/?token=" + TOKEN + "%2f"));
        assertFalse(authenticator.authorize("/?token=" + TOKEN + "#fragment"));
    }

    @Test
    public void parsesProtectedTargetSeparatelyFromLegacyUnprotectedRouting() {
        MjpegRequestAuthenticator.ParseResult valid = MjpegRequestAuthenticator.parseProtectedTarget(
                "/stream.mjpg?token=" + TOKEN, TOKEN);
        MjpegRequestAuthenticator.ParseResult malformed = MjpegRequestAuthenticator.parseProtectedTarget(
                "/stream.mjpg?token=" + TOKEN + "&token=" + TOKEN, TOKEN);

        assertEquals("/stream.mjpg", valid.path);
        assertTrue(valid.authorized);
        assertTrue(malformed.malformed);
        assertFalse(malformed.authorized);
    }
}
