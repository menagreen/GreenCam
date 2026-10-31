using GreenCam.Core;

static void Equal<T>(T expected, T actual)
{
    if (!EqualityComparer<T>.Default.Equals(expected, actual))
        throw new Exception($"Expected {expected}, got {actual}");
}

Equal("192.168.1.42", PairingAddress.Validate("192.168.1.42", "123456"));
try { PairingAddress.Validate("8.8.8.8", "123456"); throw new Exception("Public IP accepted"); }
catch (ArgumentException) { }
try { PairingAddress.Validate("192.168.1.42", "12345"); throw new Exception("Short code accepted"); }
catch (ArgumentException) { }

var header = System.Text.Encoding.ASCII.GetBytes("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: 4\r\n\r\n");
var bytes = header.Concat(new byte[] { 0xff, 0xd8, 0xff, 0xd9, 13, 10 }).ToArray();
using var stream = new MemoryStream(bytes);
var frame = await MjpegFrameReader.ReadNextAsync(stream, CancellationToken.None);
Equal(4, frame.Length);
Equal((byte)0xff, frame[0]);
Equal((byte)0xd9, frame[3]);
using var secondStream = new MemoryStream(bytes.Concat(bytes).ToArray());
Equal(4, (await MjpegFrameReader.ReadNextAsync(secondStream, CancellationToken.None)).Length);
Equal(4, (await MjpegFrameReader.ReadNextAsync(secondStream, CancellationToken.None)).Length);
var oversized = System.Text.Encoding.ASCII.GetBytes(
    "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: 4194305\r\n\r\n");
try
{
    await MjpegFrameReader.ReadNextAsync(new MemoryStream(oversized), CancellationToken.None);
    throw new Exception("Oversized frame accepted");
}
catch (InvalidDataException) { }
using var http = new HttpClient(new StubHandler());
using var client = new PairingClient(http);
var session = await client.PairAsync("192.168.1.42", "123456", CancellationToken.None);
Equal("192.168.1.42", session.Host);
Equal(8765, session.Port);
Console.WriteLine("All desktop core tests passed.");

sealed class StubHandler : HttpMessageHandler
{
    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        if (request.Method != HttpMethod.Post || request.RequestUri?.AbsoluteUri
            != "http://192.168.1.42:8766/pair?code=123456")
            throw new Exception("Unexpected pairing request");
        return Task.FromResult(new HttpResponseMessage(System.Net.HttpStatusCode.OK)
        {
            Content = new StringContent("{\"token\":\"0123456789abcdef0123456789abcdef\",\"port\":8765}")
        });
    }
}
