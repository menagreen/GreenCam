using GreenCam.Core;

namespace GreenCam.Desktop;

public sealed class MainForm : Form
{
    private readonly TextBox phoneAddress = new() { Name = "phoneAddress", AccessibleName = "Phone IP", Width = 220, PlaceholderText = "192.168.1.42" };
    private readonly TextBox pairingCode = new() { Name = "pairingCode", AccessibleName = "Pairing code", Width = 120, MaxLength = 6, PlaceholderText = "123456" };
    private readonly Button connect = new() { Name = "pairButton", AccessibleName = "Pair or disconnect", Text = "Pair", Width = 120, Height = 36 };
    private readonly Button copyObsUrl = new() { Name = "copyObsUrlButton", AccessibleName = "Copy OBS URL", Text = "Copy OBS URL", Enabled = false, Width = 145, Height = 36 };
    private readonly Button previewDisconnect = new() { Text = "Disconnect", Enabled = false, Width = 120, Height = 36 };
    private readonly TabControl navigation = new() { Name = "mainNavigation", Dock = DockStyle.Fill };
    private readonly TabPage connectionPage = new("Connection");
    private readonly TabPage previewPage = new("Preview & OBS");
    private readonly TabPage settingsPage = new("Settings");
    private readonly PictureBox preview = new()
    {
        Name = "previewImage",
        AccessibleName = "Camera preview",
        Dock = DockStyle.Fill,
        BackColor = Color.Black,
        SizeMode = PictureBoxSizeMode.Zoom
    };
    private readonly Label status = new() { Text = "Disconnected", AutoSize = true };
    private CancellationTokenSource? connection;
    private PairingSession? activeSession;

    public MainForm()
    {
        Text = "GreenCam Desktop";
        MinimumSize = new Size(760, 540);
        StartPosition = FormStartPosition.CenterScreen;
        Font = new Font("Segoe UI", 10);

        var form = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            ColumnCount = 1,
            RowCount = 3,
            Padding = new Padding(16),
            BackColor = Color.FromArgb(247, 249, 250)
        };
        form.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        form.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        form.RowStyles.Add(new RowStyle(SizeType.AutoSize));

        var header = new FlowLayoutPanel
        {
            AutoSize = true, Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown,
            WrapContents = false, Margin = new Padding(0, 0, 0, 12)
        };
        header.Controls.Add(new Label
        {
            Text = "GreenCam", AutoSize = true,
            Font = new Font("Segoe UI", 21, FontStyle.Bold), ForeColor = Color.FromArgb(20, 104, 85)
        });
        header.Controls.Add(new Label { Text = "Your phone camera, ready for content creation", AutoSize = true });
        form.Controls.Add(header, 0, 0);

        var connectionLayout = new TableLayoutPanel
        {
            Dock = DockStyle.Top, AutoSize = true, ColumnCount = 2,
            Padding = new Padding(18), BackColor = Color.White
        };
        connectionLayout.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 150));
        connectionLayout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        connectionLayout.Controls.Add(new Label { Text = "Phone IP", AutoSize = true, Anchor = AnchorStyles.Left }, 0, 0);
        connectionLayout.Controls.Add(phoneAddress, 1, 0);
        connectionLayout.Controls.Add(new Label { Text = "Pairing code", AutoSize = true, Anchor = AnchorStyles.Left }, 0, 1);
        connectionLayout.Controls.Add(pairingCode, 1, 1);
        connectionLayout.Controls.Add(connect, 1, 2);
        var connectionHelp = new Label
        {
            AutoSize = true, MaximumSize = new Size(600, 0),
            Text = "Start the camera and Wi-Fi or USB tether stream on your phone. Tap Pair with PC, enter the phone IP and code here, then approve on the phone. Use a trusted connection: HTTP is not encrypted.",
            Margin = new Padding(0, 18, 0, 0)
        };
        connectionLayout.Controls.Add(connectionHelp, 0, 3);
        connectionLayout.SetColumnSpan(connectionHelp, 2);
        connectionPage.Controls.Add(connectionLayout);

        var previewLayout = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 1, RowCount = 2, Padding = new Padding(12) };
        previewLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        previewLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        var previewActions = new FlowLayoutPanel { AutoSize = true, Dock = DockStyle.Fill };
        previewActions.Controls.Add(copyObsUrl);
        previewActions.Controls.Add(previewDisconnect);
        previewActions.Controls.Add(new Label
        {
            Text = "OBS URLs contain a bearer token and remain on the clipboard until replaced.",
            AutoSize = true, Margin = new Padding(16, 9, 0, 0)
        });
        previewLayout.Controls.Add(previewActions, 0, 0);
        previewLayout.Controls.Add(preview, 0, 1);
        previewPage.Controls.Add(previewLayout);

        var settingsLayout = new FlowLayoutPanel
        {
            Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown,
            WrapContents = false, Padding = new Padding(18)
        };
        settingsLayout.Controls.Add(new Label { Text = "About GreenCam", AutoSize = true, Font = new Font("Segoe UI", 15, FontStyle.Bold) });
        settingsLayout.Controls.Add(new Label { Text = AboutText.Message, AutoSize = true, MaximumSize = new Size(600, 0) });
        var about = new Button { Name = "aboutButton", Text = "About", Width = 120, Height = 36, Margin = new Padding(0, 16, 0, 0) };
        about.Click += (_, _) => MessageBox.Show(this, AboutText.Message, "About GreenCam",
            MessageBoxButtons.OK, MessageBoxIcon.Information);
        settingsLayout.Controls.Add(about);
        settingsPage.Controls.Add(settingsLayout);

        navigation.TabPages.AddRange(new[] { connectionPage, previewPage, settingsPage });
        form.Controls.Add(navigation, 0, 1);

        var footer = new FlowLayoutPanel { AutoSize = true, Dock = DockStyle.Fill, Padding = new Padding(0, 10, 0, 0) };
        footer.Controls.Add(status);
        form.Controls.Add(footer, 0, 2);
        Controls.Add(form);

        connect.Click += (_, _) =>
        {
            if (connection is not null) connection.Cancel();
            else _ = ConnectAsync();
        };
        previewDisconnect.Click += (_, _) => connection?.Cancel();
        copyObsUrl.Click += (_, _) =>
        {
            if (activeSession is null) return;
            try
            {
                Clipboard.SetText(ObsUrlPolicy.Create(activeSession));
                status.Text = "OBS URL copied. Paste it into OBS Browser Source; protect your clipboard.";
            }
            catch (Exception)
            {
                status.Text = "Could not copy the OBS URL. Try again.";
            }
        };
        FormClosing += (_, _) => connection?.Cancel();
    }

    private async Task ConnectAsync()
    {
        try
        {
            PairingAddress.Validate(phoneAddress.Text, pairingCode.Text);
        }
        catch (ArgumentException exception)
        {
            status.Text = exception.Message;
            return;
        }

        connection = new CancellationTokenSource();
        var cancellationToken = connection.Token;
        connect.Text = "Disconnect";
        phoneAddress.Enabled = false;
        pairingCode.Enabled = false;

        try
        {
            using var http = new HttpClient(PairingClient.CreateLanHandler())
            {
                Timeout = Timeout.InfiniteTimeSpan
            };
            using var client = new PairingClient(http);
            status.Text = "Waiting for approval on the phone…";
            var session = await client.PairAsync(phoneAddress.Text, pairingCode.Text, cancellationToken);
            activeSession = session;
            copyObsUrl.Enabled = true;
            previewDisconnect.Enabled = true;
            pairingCode.Clear();
            status.Text = "Connected. Receiving video…";
            navigation.SelectedTab = previewPage;
            using var response = await client.OpenStreamAsync(session, cancellationToken);
            await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
            using var mailbox = new LatestFrameMailbox();
            using var renderTimer = new System.Windows.Forms.Timer { Interval = 33 };
            renderTimer.Tick += (_, _) =>
            {
                var frame = mailbox.TakeLatest();
                if (frame is null) return;
                try
                {
                    using var imageBytes = new MemoryStream(frame);
                    using var decoded = Image.FromStream(imageBytes);
                    var next = new Bitmap(decoded);
                    var previous = preview.Image;
                    preview.Image = next;
                    previous?.Dispose();
                }
                catch (ArgumentException)
                {
                    connection?.Cancel();
                }
                catch (OutOfMemoryException)
                {
                    connection?.Cancel();
                }
            };
            renderTimer.Start();
            try
            {
                await Task.Run(async () =>
                {
                    while (!cancellationToken.IsCancellationRequested)
                    {
                        var frame = await MjpegFrameReader.ReadNextAsync(stream, cancellationToken);
                        if (!mailbox.Publish(frame)) break;
                    }
                }, cancellationToken);
            }
            finally
            {
                renderTimer.Stop();
            }
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            status.Text = "Disconnected";
        }
        catch (Exception)
        {
            // Never display exception details: HTTP errors can include a tokenized URI.
            status.Text = "Connection failed. Check the code, phone stream, and trusted Wi-Fi.";
        }
        finally
        {
            activeSession = null;
            copyObsUrl.Enabled = false;
            previewDisconnect.Enabled = false;
            connection.Dispose();
            connection = null;
            connect.Text = "Pair";
            phoneAddress.Enabled = true;
            pairingCode.Enabled = true;
            var previous = preview.Image;
            preview.Image = null;
            previous?.Dispose();
            navigation.SelectedTab = connectionPage;
        }
    }
}
