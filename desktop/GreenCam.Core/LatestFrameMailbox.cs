namespace GreenCam.Core;

/// <summary>Keeps only the newest encoded frame until a renderer takes it.</summary>
public sealed class LatestFrameMailbox : IDisposable
{
    private readonly object gate = new();
    private byte[]? latest;
    private long droppedFrames;
    private bool closed;

    public long DroppedFrames
    {
        get { lock (gate) return droppedFrames; }
    }

    public bool Publish(byte[] frame)
    {
        ArgumentNullException.ThrowIfNull(frame);
        lock (gate)
        {
            if (closed) return false;
            if (latest is not null) droppedFrames++;
            latest = frame;
            return true;
        }
    }

    public byte[]? TakeLatest()
    {
        lock (gate)
        {
            var frame = latest;
            latest = null;
            return frame;
        }
    }

    public void Dispose()
    {
        lock (gate)
        {
            closed = true;
            latest = null;
        }
    }
}
