package com.example.usbwebcam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/** Small, opt-in pairing listener; never binds to a wildcard address. */
final class PairingHttpServer implements AutoCloseable {
    static final int PORT = 8766;
    private final InetAddress address;
    private final int port;
    private final PairingGate gate;
    private final Runnable onPending;
    private final Runnable onRevoked;
    private ServerSocket server;
    private Thread thread;

    PairingHttpServer(InetAddress address, PairingGate gate, Runnable onPending) {
        this(address, PORT, gate, onPending, () -> { });
    }

    PairingHttpServer(InetAddress address, int port, PairingGate gate, Runnable onPending) {
        this(address, port, gate, onPending, () -> { });
    }

    PairingHttpServer(InetAddress address, PairingGate gate, Runnable onPending,
                      Runnable onRevoked) {
        this(address, PORT, gate, onPending, onRevoked);
    }

    PairingHttpServer(InetAddress address, int port, PairingGate gate, Runnable onPending,
                      Runnable onRevoked) {
        this.address = address;
        this.port = port;
        this.gate = gate;
        this.onPending = onPending;
        this.onRevoked = onRevoked;
    }

    synchronized void start() throws IOException {
        if (server != null) return;
        ServerSocket socket = new ServerSocket();
        try {
            socket.bind(new InetSocketAddress(address, port), 1);
        } catch (IOException exception) {
            socket.close();
            throw exception;
        }
        server = socket;
        thread = new Thread(this::acceptLoop, "greencam-pairing");
        thread.setDaemon(true);
        thread.start();
    }

    synchronized boolean isRunning() {
        return server != null && !server.isClosed();
    }

    synchronized int getLocalPort() {
        return server == null ? -1 : server.getLocalPort();
    }

    private void acceptLoop() {
        while (isRunning()) {
            try (Socket client = server.accept()) {
                client.setSoTimeout(3000);
                handle(client);
            } catch (SocketException exception) {
                return;
            } catch (IOException ignored) {
                // Malformed or disconnected clients never affect the camera stream.
            }
        }
    }

    private void handle(Socket client) throws IOException {
        InputStream input = client.getInputStream();
        String requestLine = readLine(input, 128);
        if (requestLine == null) {
            reply(client, 400, "Bad Request", "");
            return;
        }
        int bytes = requestLine.length();
        String line;
        boolean origin = false;
        while (true) {
            line = readLine(input, 4096 - bytes);
            if (line == null || (bytes += line.length() + 2) > 4096) {
                reply(client, 400, "Bad Request", "");
                return;
            }
            if (line.isEmpty()) break;
            if (line.regionMatches(true, 0, "Origin:", 0, 7)) origin = true;
        }
        if (origin) {
            reply(client, 403, "Forbidden", "");
            return;
        }
        // POST has no body. A code is only an invitation; the phone must approve.
        String prefix = "POST /pair?code=";
        String suffix = " HTTP/1.1";
        if (!requestLine.startsWith(prefix) || !requestLine.endsWith(suffix)) {
            reply(client, 400, "Bad Request", "");
            return;
        }
        String code = requestLine.substring(prefix.length(), requestLine.length() - suffix.length());
        if (!code.matches("[0-9]{6}") || !gate.submit(code,
                client.getInetAddress().getHostAddress())) {
            if (!gate.isInvitationActive()) onRevoked.run();
            reply(client, 403, "Forbidden", "");
            return;
        }
        onPending.run();
        String token = gate.awaitDecision(120_000);
        if (token == null) {
            reply(client, 403, "Forbidden", "");
        } else {
            reply(client, 200, "OK", "{\"token\":\"" + token + "\",\"port\":8765}");
        }
    }

    private String readLine(InputStream input, int limit) throws IOException {
        if (limit <= 0) return null;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (bytes.size() <= limit) {
            int value = input.read();
            if (value < 0) return null;
            if (value == '\n') {
                byte[] line = bytes.toByteArray();
                int length = line.length > 0 && line[line.length - 1] == '\r'
                        ? line.length - 1 : line.length;
                return new String(line, 0, length, StandardCharsets.US_ASCII);
            }
            bytes.write(value);
        }
        return null;
    }

    private void reply(Socket client, int status, String reason, String body) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.US_ASCII);
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: application/json\r\nCache-Control: no-store\r\n"
                + "Content-Length: " + content.length + "\r\nConnection: close\r\n\r\n";
        OutputStream output = client.getOutputStream();
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(content);
        output.flush();
    }

    @Override public synchronized void close() {
        gate.revoke();
        if (server != null) {
            try { server.close(); } catch (IOException ignored) { }
            server = null;
        }
        if (thread != null) thread.interrupt();
    }
}
