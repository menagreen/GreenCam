using System.Net;
using System.Net.Sockets;

namespace GreenCam.Core;

public static class PairingAddress
{
    public static string Validate(string host, string code)
    {
        if (!IPAddress.TryParse(host.Trim(), out var address)
            || address.AddressFamily != AddressFamily.InterNetwork
            || !IsPrivate(address))
            throw new ArgumentException("Enter a private LAN IPv4 address.");
        if (code.Length != 6 || !code.All(char.IsAsciiDigit))
            throw new ArgumentException("Enter the six-digit pairing code.");
        return address.ToString();
    }

    private static bool IsPrivate(IPAddress address)
    {
        var parts = address.GetAddressBytes();
        return parts[0] == 10 || parts[0] == 192 && parts[1] == 168
            || parts[0] == 172 && parts[1] is >= 16 and <= 31;
    }
}
