using System.Globalization;
using System.Text;

namespace GreenCam.Core;

public static class MjpegFrameReader
{
    public const int MaxFrameBytes = 4 * 1024 * 1024;

    public static async Task<byte[]> ReadNextAsync(Stream stream, CancellationToken cancellationToken)
    {
        string? boundary;
        do
        {
            boundary = await ReadLineAsync(stream, cancellationToken).ConfigureAwait(false);
            if (boundary is null) throw new EndOfStreamException();
        } while (boundary.Length == 0);
        if (boundary != "--frame") throw new InvalidDataException("Unexpected MJPEG boundary.");

        var contentType = await ReadLineAsync(stream, cancellationToken).ConfigureAwait(false);
        var lengthLine = await ReadLineAsync(stream, cancellationToken).ConfigureAwait(false);
        var blank = await ReadLineAsync(stream, cancellationToken).ConfigureAwait(false);
        if (!string.Equals(contentType, "Content-Type: image/jpeg", StringComparison.OrdinalIgnoreCase)
            || lengthLine is null || !lengthLine.StartsWith("Content-Length: ", StringComparison.OrdinalIgnoreCase)
            || blank != ""
            || !int.TryParse(lengthLine.AsSpan("Content-Length: ".Length), NumberStyles.None,
                CultureInfo.InvariantCulture, out var length)
            || length is < 4 or > MaxFrameBytes)
            throw new InvalidDataException("Invalid MJPEG frame headers.");

        var frame = new byte[length];
        await stream.ReadExactlyAsync(frame, cancellationToken).ConfigureAwait(false);
        if (frame[0] != 0xff || frame[1] != 0xd8
            || frame[^2] != 0xff || frame[^1] != 0xd9)
            throw new InvalidDataException("Invalid JPEG markers.");
        return frame;
    }

    private static async Task<string?> ReadLineAsync(Stream stream, CancellationToken cancellationToken)
    {
        using var bytes = new MemoryStream();
        var one = new byte[1];
        while (bytes.Length <= 256)
        {
            var count = await stream.ReadAsync(one, cancellationToken).ConfigureAwait(false);
            if (count == 0) return null;
            if (one[0] == '\n')
            {
                var data = bytes.ToArray();
                var length = data.Length > 0 && data[^1] == '\r' ? data.Length - 1 : data.Length;
                return Encoding.ASCII.GetString(data, 0, length);
            }
            bytes.WriteByte(one[0]);
        }
        throw new InvalidDataException("MJPEG header exceeds limit.");
    }
}
