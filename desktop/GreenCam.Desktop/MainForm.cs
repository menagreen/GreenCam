using GreenCam.Core;

namespace GreenCam.Desktop;

public sealed class MainForm : Form
{
    private readonly TextBox phoneAddress = new() { Width = 160, PlaceholderText = "192.168.1.42" };
    private readonly TextBox pairingCode = new() { Width = 90, MaxLength = 6, PlaceholderText = "123456" };
    private readonly Button connect = new() { Text = "Pair", Width = 95 };
    private readonly PictureBox preview = new()
    {
        Dock = DockStyle.Fill,
        BackColor = Color.Black,
        SizeMode = PictureBoxSizeMode.Zoom
    };
    private readonly Label status = new() { Text = "Disconnected", AutoSize = true };
    private CancellationTokenSource? connection;

    public MainForm()
    {
        Text = "GreenCam Desktop";
        MinimumSize = new Size(740, 520);
        StartPosition = FormStartPosition.CenterScreen;

        var form = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            ColumnCount = 1,
            RowCount = 3,
            Padding = new Padding(12)
        };
        form.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        form.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        form.RowStyles.Add(new RowStyle(SizeType.AutoSize));

        var top = new FlowLayoutPanel { AutoSize = true, Dock = DockStyle.Fill, WrapContents = true };
        top.Controls.Add(new Label { Text = "Phone IP", AutoSize = true, Margin = new Padding(0, 7, 5, 0) });
        top.Controls.Add(phoneAddress);
        top.Controls.Add(new Label { Text = "Pairing code", AutoSize = true, Margin = new Padding(16, 7, 5, 0) });
        top.Controls.Add(pairingCode);
        top.Controls.Add(connect);
        form.Controls.Add(top, 0, 0);
        form.Controls.Add(preview, 0, 1);

        var footer = new FlowLayoutPanel { AutoSize = true, Dock = DockStyle.Fill, WrapContents = true };
        footer.Controls.Add(status);
        footer.Controls.Add(new Label
        {
            Text = "Use only on trusted Wi-Fi. Video and pairing are not encrypted.",
            AutoSize = true,
            Margin = new Padding(22, 0, 0, 0)
        });
        form.Controls.Add(footer, 0, 2);
        Controls.Add(form);

        connect.Click += (_, _) =>
        {
            if (connection is not null) connection.Cancel();
            else _ = ConnectAsync();
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
            using var http = new HttpClient { Timeout = Timeout.InfiniteTimeSpan };
            using var client = new PairingClient(http);
            status.Text = "Waiting for approval on the phone…";
            var session = await client.PairAsync(phoneAddress.Text, pairingCode.Text, cancellationToken);
            pairingCode.Clear();
            status.Text = "Connected. Receiving video…";
            using var response = await client.OpenStreamAsync(session, cancellationToken);
            await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
            while (!cancellationToken.IsCancellationRequested)
            {
                var frame = await MjpegFrameReader.ReadNextAsync(stream, cancellationToken);
                using var imageBytes = new MemoryStream(frame);
                using var decoded = Image.FromStream(imageBytes);
                var next = new Bitmap(decoded);
                var previous = preview.Image;
                preview.Image = next;
                previous?.Dispose();
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
            connection.Dispose();
            connection = null;
            connect.Text = "Pair";
            phoneAddress.Enabled = true;
            pairingCode.Enabled = true;
            var previous = preview.Image;
            preview.Image = null;
            previous?.Dispose();
        }
    }
}
