package com.example.usbwebcam;

import static org.junit.Assert.*;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class PairingHttpServerTest {
    @Test public void returnsTokenOnlyAfterPhoneApproval() throws Exception {
        PairingGate gate = new PairingGate(System::currentTimeMillis);
        String code = gate.begin("0123456789abcdef0123456789abcdef");
        CountDownLatch pending = new CountDownLatch(1);
        try (PairingHttpServer server = new PairingHttpServer(
                InetAddress.getByName("127.0.0.1"), 0, gate, pending::countDown)) {
            server.start();
            int port = server.getLocalPort();
            assertEquals(403, request(port, "wrong").getResponseCode());
            AtomicReference<String> result = new AtomicReference<>();
            Thread client = new Thread(() -> {
                try {
                    HttpURLConnection connection = request(port, code);
                    if (connection.getResponseCode() == 200) {
                        result.set(new String(connection.getInputStream().readAllBytes(),
                                StandardCharsets.US_ASCII));
                    }
                } catch (Exception ignored) { }
            });
            client.start();
            assertTrue(pending.await(3, TimeUnit.SECONDS));
            assertNull(result.get());
            assertTrue(gate.approve());
            client.join(3000);
            assertEquals("{\"token\":\"0123456789abcdef0123456789abcdef\",\"port\":8765}", result.get());
            assertEquals(403, request(port, code).getResponseCode());
        }
    }

    private static HttpURLConnection request(int port, String code) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "http://127.0.0.1:" + port + "/pair?code=" + code).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        return connection;
    }
}
