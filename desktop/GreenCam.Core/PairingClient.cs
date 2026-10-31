using System.Net.Http.Headers;
using System.Text.Json;

namespace GreenCam.Core;

public sealed record PairingSession(string Host, string Token, int Port);

public sealed class PairingClient(HttpClient http) : IDisposable
{
    public async Task<PairingSession> PairAsync(string host, string code,
        CancellationToken cancellationToken)
    {
        host = PairingAddress.Validate(host, code);
        using var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        deadline.CancelAfter(TimeSpan.FromSeconds(125));
        using var request = new HttpRequestMessage(HttpMethod.Post,
            $"http://{host}:8766/pair?code={code}");
        using var response = await http.SendAsync(request, deadline.Token).ConfigureAwait(false);
        if (!response.IsSuccessStatusCode)
            throw new InvalidOperationException("Pairing was denied or expired. Start a new invitation on the phone.");

        var json = await response.Content.ReadAsStringAsync(deadline.Token).ConfigureAwait(false);
        var result = JsonSerializer.Deserialize<PairingReply>(json,
            new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
        if (result?.Token is null || !System.Text.RegularExpressions.Regex.IsMatch(
                result.Token, "^[0-9a-f]{32}$") || result.Port != 8765)
            throw new InvalidDataException("The phone returned an invalid pairing response.");
        return new PairingSession(host, result.Token, result.Port);
    }

    public async Task<HttpResponseMessage> OpenStreamAsync(PairingSession session,
        CancellationToken cancellationToken)
    {
        var response = await http.GetAsync(
            $"http://{session.Host}:{session.Port}/stream.mjpg?token={session.Token}",
            HttpCompletionOption.ResponseHeadersRead, cancellationToken).ConfigureAwait(false);
        if (!response.IsSuccessStatusCode || response.Content.Headers.ContentType?.MediaType
            != "multipart/x-mixed-replace")
        {
            response.Dispose();
            throw new InvalidOperationException("The camera stream is unavailable. Check that GreenCam is running.");
        }
        return response;
    }

    public void Dispose() => http.Dispose();

    private sealed record PairingReply(string Token, int Port);
}
