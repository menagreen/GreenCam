package com.example.usbwebcam;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class MjpegHttpServerTest {
    @Test
    public void servesIndexAndMjpegFrameOnLoopback() throws Exception {
        LatestJpegFrameStore frames = new LatestJpegFrameStore();
        long session = frames.beginSession();
        byte[] jpeg = new byte[] {(byte) 0xff, (byte) 0xd8, 0x45, (byte) 0xff, (byte) 0xd9};
        frames.publish(session, jpeg);
        MjpegHttpServer server = new MjpegHttpServer(frames, 0);
        server.start();

        try {
            String index = request(server.getLocalPort(), "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
            assertTrue(index.startsWith("HTTP/1.1 200 OK\r\n"));
            assertTrue(index.contains("src=\"/stream.mjpg\""));

            try (Socket socket = connect(server.getLocalPort())) {
                OutputStream output = socket.getOutputStream();
                output.write("GET /stream.mjpg HTTP/1.1\r\nHost: localhost\r\n\r\n"
                        .getBytes(StandardCharsets.US_ASCII));
                output.flush();
                InputStream input = socket.getInputStream();
                String headers = readHeaders(input);
                assertTrue(headers.startsWith("HTTP/1.1 200 OK\r\n"));
                assertTrue(headers.contains("boundary=frame\r\n"));
                assertTrue(readLine(input).equals("--frame"));
                assertTrue(readLine(input).equals("Content-Type: image/jpeg"));
                int contentLength = Integer.parseInt(readLine(input).substring("Content-Length: ".length()));
                assertTrue(readLine(input).isEmpty());
                byte[] received = new byte[contentLength];
                new DataInputStream(input).readFully(received);
                assertArrayEquals(jpeg, received);
            }
        } finally {
            server.close();
        }
    }

    @Test
    public void rejectsSecondStreamClientAndClosesActiveStreamOnStop() throws Exception {
        LatestJpegFrameStore frames = new LatestJpegFrameStore();
        frames.beginSession();
        MjpegHttpServer server = new MjpegHttpServer(frames, 0);
        server.start();

        try (Socket first = connect(server.getLocalPort())) {
            first.getOutputStream().write("GET /stream.mjpg HTTP/1.1\r\n\r\n"
                    .getBytes(StandardCharsets.US_ASCII));
            first.getOutputStream().flush();
            String streamHeaders = readHeaders(first.getInputStream());
            assertTrue(streamHeaders.startsWith("HTTP/1.1 200 OK\r\n"));

            String secondResponse = request(server.getLocalPort(),
                    "GET /stream.mjpg HTTP/1.1\r\n\r\n");
            assertTrue(secondResponse.startsWith("HTTP/1.1 503 Service Unavailable\r\n"));

            server.close();
            assertTrue(first.getInputStream().read() == -1);
        } finally {
            server.close();
        }
    }

    private static Socket connect(int port) throws Exception {
        Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), port);
        socket.setSoTimeout(3000);
        return socket;
    }

    private static String request(int port, String request) throws Exception {
        try (Socket socket = connect(port)) {
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String readHeaders(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int matched = 0;
        byte[] end = new byte[] {'\r', '\n', '\r', '\n'};
        while (matched < end.length) {
            int value = input.read();
            if (value < 0) {
                throw new IllegalStateException("Connection ended before response headers completed");
            }
            bytes.write(value);
            matched = value == (end[matched] & 0xff) ? matched + 1 : value == '\r' ? 1 : 0;
        }
        return new String(bytes.toByteArray(), StandardCharsets.US_ASCII);
    }

    private static String readLine(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int previous = -1;
        while (true) {
            int value = input.read();
            if (value < 0) {
                throw new IllegalStateException("Connection ended before multipart headers completed");
            }
            if (previous == '\r' && value == '\n') {
                byte[] line = bytes.toByteArray();
                return new String(line, 0, line.length - 1, StandardCharsets.US_ASCII);
            }
            bytes.write(value);
            previous = value;
        }
    }
}
