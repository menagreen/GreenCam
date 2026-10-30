package com.example.usbwebcam;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class MjpegHttpProtocol {
    enum Route {
        INDEX,
        STREAM,
        METHOD_NOT_ALLOWED,
        NOT_FOUND,
        UNAUTHORIZED,
        BAD_REQUEST
    }

    private static final String BOUNDARY = "frame";
    private static final byte[] INDEX_PAGE = ("<!doctype html><html><head>"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<title>USB Webcam</title></head><body><img alt=\"Camera stream\" "
            + "style=\"width:100%;height:auto\" src=\"/stream.mjpg\"></body></html>")
            .getBytes(StandardCharsets.UTF_8);

    private MjpegHttpProtocol() { }

    static Route route(String method, String path) {
        if (!"GET".equals(method)) {
            return Route.METHOD_NOT_ALLOWED;
        }
        if ("/".equals(path)) {
            return Route.INDEX;
        }
        if ("/stream.mjpg".equals(path)) {
            return Route.STREAM;
        }
        return Route.NOT_FOUND;
    }

    static String indexPage() {
        return new String(INDEX_PAGE, StandardCharsets.UTF_8);
    }

    static byte[] indexResponse() {
        return response(200, "OK", "text/html; charset=utf-8", INDEX_PAGE);
    }

    static byte[] indexResponse(String token) {
        return response(200, "OK", "text/html; charset=utf-8",
                indexPage(token).getBytes(StandardCharsets.UTF_8));
    }

    static String indexPage(String token) {
        if (token == null || !token.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("Token must contain 128 bits encoded as lowercase hex");
        }
        return indexPage().replace("src=\"/stream.mjpg\"", "src=\"/stream.mjpg?token=" + token + "\"");
    }

    static byte[] streamHeaders() {
        return ("HTTP/1.1 200 OK\r\n"
                + "Content-Type: multipart/x-mixed-replace; boundary=" + BOUNDARY + "\r\n"
                + "Cache-Control: no-store, no-cache, must-revalidate\r\n"
                + "Pragma: no-cache\r\n"
                + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] framePart(byte[] jpeg) {
        if (jpeg == null || jpeg.length == 0 || jpeg.length > LatestJpegFrameStore.MAX_JPEG_BYTES) {
            throw new IllegalArgumentException("JPEG frame is empty or exceeds the size limit");
        }
        byte[] header = ("--" + BOUNDARY + "\r\n"
                + "Content-Type: image/jpeg\r\n"
                + "Content-Length: " + jpeg.length + "\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream output = new ByteArrayOutputStream(header.length + jpeg.length + 2);
        output.write(header, 0, header.length);
        output.write(jpeg, 0, jpeg.length);
        output.write('\r');
        output.write('\n');
        return output.toByteArray();
    }

    static byte[] errorResponse(Route route) {
        if (route == Route.UNAUTHORIZED) {
            return response(401, "Unauthorized", "text/plain; charset=utf-8",
                    "Unauthorized".getBytes(StandardCharsets.UTF_8));
        }
        if (route == Route.METHOD_NOT_ALLOWED) {
            return response(405, "Method Not Allowed", "text/plain; charset=utf-8",
                    "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        }
        if (route == Route.NOT_FOUND) {
            return response(404, "Not Found", "text/plain; charset=utf-8",
                    "Not Found".getBytes(StandardCharsets.UTF_8));
        }
        return response(400, "Bad Request", "text/plain; charset=utf-8",
                "Bad Request".getBytes(StandardCharsets.UTF_8));
    }

    static byte[] busyResponse() {
        return response(503, "Service Unavailable", "text/plain; charset=utf-8",
                "A stream client is already connected".getBytes(StandardCharsets.UTF_8));
    }

    static byte[] response(int status, String reason, String contentType, byte[] body) {
        byte[] header = ("HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream output = new ByteArrayOutputStream(header.length + body.length);
        output.write(header, 0, header.length);
        output.write(body, 0, body.length);
        return output.toByteArray();
    }
}
