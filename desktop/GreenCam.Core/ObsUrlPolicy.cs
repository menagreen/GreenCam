using System.Text.RegularExpressions;

namespace GreenCam.Core;

public static class ObsUrlPolicy
{
    public static string Create(PairingSession session)
    {
        ArgumentNullException.ThrowIfNull(session);
        var host = PairingAddress.Validate(session.Host, "000000");
        if (session.Port != 8765 || !Regex.IsMatch(session.Token, "^[0-9a-f]{32}$"))
            throw new ArgumentException("Invalid paired session.", nameof(session));
        return $"http://{host}:{session.Port}/?token={session.Token}";
    }
}
