package com.example.usbwebcam;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

final class YuvJpegEncoder {
    private static final int MAX_DIMENSION = 1280;
    private static final int MAX_PIXELS = 1280 * 720;
    private static final int JPEG_QUALITY = 80;

    private YuvJpegEncoder() { }

    static byte[] encode(ImageProxy image) {
        if (image == null || image.getFormat() != ImageFormat.YUV_420_888) {
            return null;
        }
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        if (sourceWidth < 2 || sourceHeight < 2) {
            return null;
        }

        int rotation = ((image.getImageInfo().getRotationDegrees() % 360) + 360) % 360;
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            rotation = 0;
        }
        int orientedWidth = rotation == 90 || rotation == 270 ? sourceHeight : sourceWidth;
        int orientedHeight = rotation == 90 || rotation == 270 ? sourceWidth : sourceHeight;
        int outputWidth = outputDimension(orientedWidth, orientedWidth, orientedHeight);
        int outputHeight = outputDimension(orientedHeight, orientedWidth, orientedHeight);
        outputWidth = Math.max(2, outputWidth - outputWidth % 2);
        outputHeight = Math.max(2, outputHeight - outputHeight % 2);

        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        if (planes.length < 3) {
            return null;
        }
        ByteBuffer yPlane = planes[0].getBuffer().duplicate();
        ByteBuffer uPlane = planes[1].getBuffer().duplicate();
        ByteBuffer vPlane = planes[2].getBuffer().duplicate();
        int yBase = yPlane.position();
        int uBase = uPlane.position();
        int vBase = vPlane.position();
        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();
        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();

        byte[] nv21 = new byte[outputWidth * outputHeight * 3 / 2];
        try {
            int outputIndex = 0;
            for (int y = 0; y < outputHeight; y++) {
                int orientedY = y * orientedHeight / outputHeight;
                for (int x = 0; x < outputWidth; x++) {
                    int orientedX = x * orientedWidth / outputWidth;
                    Point source = sourcePoint(rotation, orientedX, orientedY, sourceWidth, sourceHeight);
                    int index = yBase + source.y * yRowStride + source.x * yPixelStride;
                    nv21[outputIndex++] = yPlane.get(index);
                }
            }

            int chromaWidth = (sourceWidth + 1) / 2;
            int chromaHeight = (sourceHeight + 1) / 2;
            for (int y = 0; y < outputHeight / 2; y++) {
                int orientedY = Math.min(orientedHeight - 1, y * 2 * orientedHeight / outputHeight);
                for (int x = 0; x < outputWidth / 2; x++) {
                    int orientedX = Math.min(orientedWidth - 1, x * 2 * orientedWidth / outputWidth);
                    Point source = sourcePoint(rotation, orientedX, orientedY, sourceWidth, sourceHeight);
                    int chromaX = Math.min(chromaWidth - 1, source.x / 2);
                    int chromaY = Math.min(chromaHeight - 1, source.y / 2);
                    int vIndex = vBase + chromaY * vRowStride + chromaX * vPixelStride;
                    int uIndex = uBase + chromaY * uRowStride + chromaX * uPixelStride;
                    nv21[outputIndex++] = vPlane.get(vIndex);
                    nv21[outputIndex++] = uPlane.get(uIndex);
                }
            }
        } catch (IndexOutOfBoundsException exception) {
            return null;
        }

        ByteArrayOutputStream jpeg = new ByteArrayOutputStream(64 * 1024);
        boolean encoded = new YuvImage(nv21, ImageFormat.NV21, outputWidth, outputHeight, null)
                .compressToJpeg(new Rect(0, 0, outputWidth, outputHeight), JPEG_QUALITY, jpeg);
        if (!encoded || jpeg.size() == 0 || jpeg.size() > LatestJpegFrameStore.MAX_JPEG_BYTES) {
            return null;
        }
        return jpeg.toByteArray();
    }

    private static int outputDimension(int dimension, int width, int height) {
        double scale = Math.min(1.0, Math.min((double) MAX_DIMENSION / width,
                Math.min((double) MAX_DIMENSION / height,
                        Math.sqrt((double) MAX_PIXELS / ((long) width * height)))));
        return Math.max(2, (int) Math.floor(dimension * scale));
    }

    private static Point sourcePoint(int rotation, int x, int y, int width, int height) {
        switch (rotation) {
            case 90:
                return new Point(y, height - 1 - x);
            case 180:
                return new Point(width - 1 - x, height - 1 - y);
            case 270:
                return new Point(width - 1 - y, x);
            default:
                return new Point(x, y);
        }
    }

    private static final class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
