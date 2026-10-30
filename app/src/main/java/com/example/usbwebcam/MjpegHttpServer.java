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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class MjpegHttpServer implements AutoCloseable {
    static final int PORT = 8765;
    private static final int MAX_REQUEST_LINE_BYTES = 1024;
    private static final int MAX_HEADER_BYTES = 4096;

    private final LatestJpegFrameStore frames;
    private final int port;
    private final MjpegRequestAuthenticator authenticator;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<Socket> activeStream = new AtomicReference<>();
    private final ExecutorService streamExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "webcam-mjpeg-stream");
        thread.setDaemon(true);
        return thread;
    });
    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;

    MjpegHttpServer(LatestJpegFrameStore frames) {
        this(frames, PORT);
    }

    MjpegHttpServer(LatestJpegFrameStore frames, int port) {
        this(frames, port, null);
    }

    MjpegHttpServer(LatestJpegFrameStore frames, int port, MjpegRequestAuthenticator authenticator) {
        if (frames == null) {
            throw new IllegalArgumentException("Frame store is required");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port is out of range");
        }
        this.frames = frames;
        this.port = port;
        this.authenticator = authenticator;
    }

    synchronized void start() throws IOException {
        if (running.get()) {
            return;
        }
        ServerSocket socket = new ServerSocket();
        try {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 4);
        } catch (IOException exception) {
            closeQuietly(socket);
            throw exception;
        }
        serverSocket = socket;
        running.set(true);
        acceptThread = new Thread(this::acceptLoop, "webcam-http-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    boolean isRunning() {
        return running.get();
    }

    int getLocalPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? -1 : socket.getLocalPort();
    }

    private void acceptLoop() {
        while (running.get()) {
            Socket client = null;
            try {
                client = serverSocket.accept();
                client.setSoTimeout(3000);
                handleRequest(client);
            } catch (SocketException exception) {
                if (running.get()) {
                    closeQuietly(client);
                }
                return;
            } catch (IOException exception) {
                closeQuietly(client);
            }
        }
    }

    private void handleRequest(Socket client) throws IOException {
        boolean ownedByStream = false;
        try {
            Request request = readRequest(client.getInputStream());
            MjpegHttpProtocol.Route route = request.route;
            OutputStream output = client.getOutputStream();
            if (route == MjpegHttpProtocol.Route.INDEX) {
                output.write(authenticator == null ? MjpegHttpProtocol.indexResponse()
                        : MjpegHttpProtocol.indexResponse(authenticator.tokenForSession()));
                output.flush();
            } else if (route == MjpegHttpProtocol.Route.STREAM) {
                if (!activeStream.compareAndSet(null, client)) {
                    output.write(MjpegHttpProtocol.busyResponse());
                    output.flush();
                } else {
                    try {
                        streamExecutor.execute(() -> streamFrames(client));
                        ownedByStream = true;
                    } catch (RejectedExecutionException exception) {
                        activeStream.compareAndSet(client, null);
                        output.write(MjpegHttpProtocol.busyResponse());
                        output.flush();
                    }
                }
            } else {
                output.write(MjpegHttpProtocol.errorResponse(route));
                output.flush();
            }
        } finally {
            if (!ownedByStream) {
                closeQuietly(client);
            }
        }
    }

    private Request readRequest(InputStream input) throws IOException {
        String requestLine = readLine(input, MAX_REQUEST_LINE_BYTES);
        if (requestLine == null) {
            return new Request(MjpegHttpProtocol.Route.BAD_REQUEST);
        }
        String[] fields = requestLine.trim().split("\\s+");
        if (fields.length != 3 || !("HTTP/1.0".equals(fields[2]) || "HTTP/1.1".equals(fields[2]))) {
            return new Request(MjpegHttpProtocol.Route.BAD_REQUEST);
        }

        int headerBytes = requestLine.length();
        while (true) {
            String header = readLine(input, MAX_HEADER_BYTES - headerBytes);
            if (header == null) {
                return new Request(MjpegHttpProtocol.Route.BAD_REQUEST);
            }
            headerBytes += header.length() + 2;
            if (headerBytes > MAX_HEADER_BYTES) {
                return new Request(MjpegHttpProtocol.Route.BAD_REQUEST);
            }
            if (header.isEmpty()) {
                break;
            }
        }
        if (authenticator == null) {
            return new Request(MjpegHttpProtocol.route(fields[0], fields[1]));
        }
        MjpegRequestAuthenticator.ParseResult parsed = MjpegRequestAuthenticator.parseProtectedTarget(
                fields[1], authenticator.tokenForSession());
        if (parsed.malformed) {
            return new Request(MjpegHttpProtocol.Route.BAD_REQUEST);
        }
        if (!parsed.authorized) {
            return new Request(MjpegHttpProtocol.Route.UNAUTHORIZED);
        }
        return new Request(MjpegHttpProtocol.route(fields[0], parsed.path));
    }

    private String readLine(InputStream input, int maxBytes) throws IOException {
        if (maxBytes <= 0) {
            return null;
        }
        ByteArrayOutputStream line = new ByteArrayOutputStream(Math.min(128, maxBytes));
        while (line.size() <= maxBytes) {
            int value = input.read();
            if (value < 0) {
                return null;
            }
            if (value == '\n') {
                byte[] bytes = line.toByteArray();
                int length = bytes.length;
                if (length > 0 && bytes[length - 1] == '\r') {
                    length--;
                }
                return new String(bytes, 0, length, StandardCharsets.US_ASCII);
            }
            line.write(value);
        }
        return null;
    }

    private void streamFrames(Socket client) {
        try (OutputStream output = client.getOutputStream()) {
            output.write(MjpegHttpProtocol.streamHeaders());
            output.flush();
            long lastSequence = -1;
            while (running.get() && !client.isClosed()) {
                LatestJpegFrameStore.Frame frame;
                try {
                    frame = frames.awaitAfter(lastSequence, 1000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (frame == null) {
                    continue;
                }
                output.write(MjpegHttpProtocol.framePart(frame.jpeg));
                output.flush();
                lastSequence = frame.sequence;
            }
        } catch (IOException ignored) {
            // Client disconnects and camera stop both close the stream socket.
        } finally {
            activeStream.compareAndSet(client, null);
            closeQuietly(client);
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        closeQuietly(serverSocket);
        closeQuietly(activeStream.getAndSet(null));
        streamExecutor.shutdownNow();
        serverSocket = null;
        acceptThread = null;
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Closing sockets is best-effort during shutdown.
            }
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Closing sockets is best-effort during shutdown.
            }
        }
    }

    private static final class Request {
        final MjpegHttpProtocol.Route route;

        Request(MjpegHttpProtocol.Route route) {
            this.route = route;
        }
    }
}
