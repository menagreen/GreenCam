package com.example.usbwebcam;

final class LatestJpegFrameStore {
    static final int MAX_JPEG_BYTES = 2 * 1024 * 1024;

    private long sessionId;
    private long sequence;
    private Frame latest;

    synchronized long beginSession() {
        sessionId++;
        latest = null;
        notifyAll();
        return sessionId;
    }

    synchronized void endSession(long requestedSessionId) {
        if (requestedSessionId == sessionId) {
            sessionId++;
            latest = null;
            notifyAll();
        }
    }

    synchronized boolean publish(long requestedSessionId, byte[] jpeg) {
        if (requestedSessionId != sessionId || jpeg == null || jpeg.length == 0
                || jpeg.length > MAX_JPEG_BYTES) {
            return false;
        }
        latest = new Frame(++sequence, jpeg.clone());
        notifyAll();
        return true;
    }

    synchronized Frame awaitAfter(long lastSequence, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (latest == null || latest.sequence <= lastSequence) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                return null;
            }
            long waitMillis = remainingNanos / 1_000_000L;
            int waitNanos = (int) (remainingNanos % 1_000_000L);
            wait(waitMillis, waitNanos);
        }
        return latest;
    }

    synchronized long currentSequence() {
        return sequence;
    }

    static final class Frame {
        final long sequence;
        final byte[] jpeg;

        Frame(long sequence, byte[] jpeg) {
            this.sequence = sequence;
            this.jpeg = jpeg;
        }
    }
}
