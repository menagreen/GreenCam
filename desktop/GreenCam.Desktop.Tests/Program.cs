using GreenCam.Desktop;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        ApplicationConfiguration.Initialize();
        using var form = new MainForm();
        var tabs = form.Controls.Find("mainNavigation", true).OfType<TabControl>().Single();
        var names = tabs.TabPages.Cast<TabPage>().Select(page => page.Text).ToArray();
        if (!names.SequenceEqual(new[] { "Connection", "Preview & OBS", "Settings" }))
            throw new Exception("Expected separate Connection, Preview & OBS, and Settings pages.");
        foreach (var required in new[] { "pairButton", "previewImage", "copyObsUrlButton", "aboutButton" })
            if (form.Controls.Find(required, true).Length != 1)
                throw new Exception($"Missing accessible control: {required}");
        ExpectPage(form, "pairButton", "Connection");
        ExpectPage(form, "previewImage", "Preview & OBS");
        ExpectPage(form, "copyObsUrlButton", "Preview & OBS");
        ExpectPage(form, "aboutButton", "Settings");
        if (form.Controls.Find("copyObsUrlButton", true)[0].Enabled)
            throw new Exception("OBS URL action must be disabled before pairing.");
        Console.WriteLine("Desktop navigation tests passed.");
    }

    private static void ExpectPage(Form form, string controlName, string pageName)
    {
        Control? control = form.Controls.Find(controlName, true).Single();
        while (control is not null && control is not TabPage) control = control.Parent;
        if (control?.Text != pageName)
            throw new Exception($"{controlName} is not on {pageName}.");
    }
}
