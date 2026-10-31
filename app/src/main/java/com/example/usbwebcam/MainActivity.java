package com.example.usbwebcam;

import android.Manifest;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Size;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Camera;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ZoomState;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView statusView;
    private TextView selectionView;
    private TextView cameraCapabilityView;
    private TextView zoomValueView;
    private SeekBar zoomSeekBar;
    private Button startButton;
    private Button themeButton;
    private int themeMode;
    private Button dimScreenButton;
    private Button wifiButton;
    private TextView wifiWarningView;
    private TextView wifiUrlView;
    private Button wifiCopyButton;
    private FrameLayout dimOverlay;
    private RadioGroup cameraGroup;
    private RadioGroup fpsGroup;
    private RadioButton backButton;
    private RadioButton frontButton;
    private final Map<String, RadioButton> rearButtons = new LinkedHashMap<>();
    private String selectedRearCameraId;
    private RadioButton fps10Button;
    private RadioButton fps15Button;
    private RadioButton fps30Button;
    private RadioButton fps60Button;
    private boolean syncingControls;
    private ProcessCameraProvider cameraProvider;
    private CameraInfo boundCameraInfo;
    private Camera boundCamera;
    private float zoomMinimum = 1f;
    private float zoomMaximum = 1f;
    private int lensFacing = CameraSelectionPolicy.LENS_BACK;
    private boolean started;
    private boolean dimScreenActive;
    private float originalScreenBrightness = Float.NaN;
    private final PreviewStartGate previewStartGate = new PreviewStartGate();
    private final LatestJpegFrameStore jpegFrames = new LatestJpegFrameStore();
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();
    private MjpegHttpServer mjpegServer;
    private MjpegHttpServer wifiServer;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback wifiNetworkCallback;
    private Network wifiNetwork;
    private String wifiAddress;
    private MjpegRequestAuthenticator wifiAuthenticator;
    private boolean wifiEnabled;
    private long frameSessionId;
    private final FrameRateLimiter frameRateLimiter = new FrameRateLimiter();

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    previewStartGate.requestStart();
                    startPreview();
                } else {
                    previewStartGate.cancel();
                    statusView.setText(R.string.camera_permission_required);
                    updateControls();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeMode = ThemeModePolicy.sanitize(getPreferences(MODE_PRIVATE).getInt("theme_mode", ThemeModePolicy.SYSTEM));
        AppCompatDelegate.setDefaultNightMode(themeMode == ThemeModePolicy.DARK
                ? AppCompatDelegate.MODE_NIGHT_YES : themeMode == ThemeModePolicy.LIGHT
                ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        createLayout();
        updateControls();
        requestCameraProvider();
    }

    @Override
    protected void onStop() {
        stopPreview();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        exitDimScreenMode();
        stopWifiListener(true);
        analysisExecutor.shutdownNow();
        super.onDestroy();
    }

    private void createLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.subtitle);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        root.addView(subtitle);

        themeButton = new Button(this);
        themeButton.setOnClickListener(view -> {
            if (started) {
                return;
            }
            themeMode = ThemeModePolicy.next(themeMode);
            getPreferences(MODE_PRIVATE).edit().putInt("theme_mode", themeMode).apply();
            AppCompatDelegate.setDefaultNightMode(themeMode == ThemeModePolicy.DARK
                    ? AppCompatDelegate.MODE_NIGHT_YES : themeMode == ThemeModePolicy.LIGHT
                    ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        });
        root.addView(themeButton);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setBackgroundColor(Color.BLACK);
        int previewHeight = Math.min(dp(320), Math.max(dp(180),
                (getResources().getDisplayMetrics().widthPixels - dp(32)) * 9 / 16));
        root.addView(previewView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, previewHeight));
        previewView.setOnTouchListener((view, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP || !started || boundCamera == null) {
                return false;
            }
            boundCamera.getCameraControl().startFocusAndMetering(
                    new FocusMeteringAction.Builder(previewView.getMeteringPointFactory()
                            .createPoint(event.getX(), event.getY())).build());
            view.performClick();
            return true;
        });
        previewView.setContentDescription(getString(R.string.preview_focus_description));

        zoomValueView = new TextView(this);
        zoomValueView.setText(R.string.zoom_unavailable);
        zoomValueView.setPadding(0, dp(12), 0, 0);
        root.addView(zoomValueView);
        zoomSeekBar = new SeekBar(this);
        zoomSeekBar.setMax(100);
        zoomSeekBar.setEnabled(false);
        zoomSeekBar.setContentDescription(getString(R.string.zoom_control_description));
        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || !started || boundCamera == null
                        || !ZoomControlPolicy.supportsZoom(zoomMinimum, zoomMaximum)) {
                    return;
                }
                float ratio = ZoomControlPolicy.ratioForProgress(progress, zoomMinimum, zoomMaximum);
                boundCamera.getCameraControl().setZoomRatio(ratio);
                zoomValueView.setText(getString(R.string.zoom_value, ratio));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        root.addView(zoomSeekBar);

        statusView = new TextView(this);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        statusView.setPadding(0, dp(12), 0, dp(4));
        root.addView(statusView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        selectionView = new TextView(this);
        selectionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        root.addView(selectionView);

        cameraCapabilityView = new TextView(this);
        cameraCapabilityView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        root.addView(cameraCapabilityView);

        TextView cameraLabel = new TextView(this);
        cameraLabel.setText(R.string.camera);
        cameraLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        cameraLabel.setPadding(0, dp(16), 0, 0);
        root.addView(cameraLabel);

        cameraGroup = new RadioGroup(this);
        cameraGroup.setOrientation(RadioGroup.HORIZONTAL);
        cameraGroup.setContentDescription(getString(R.string.camera_selection));
        backButton = cameraOption(getString(R.string.rear_camera), cameraGroup);
        frontButton = cameraOption(getString(R.string.front_camera), cameraGroup);
        cameraGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!syncingControls) {
                if (checkedId == frontButton.getId()) {
                    selectCamera(CameraSelectionPolicy.LENS_FRONT);
                } else {
                    String previousRearId = selectedRearCameraId;
                    int previousLens = lensFacing;
                    for (Map.Entry<String, RadioButton> entry : rearButtons.entrySet()) {
                        if (entry.getValue().getId() == checkedId) {
                            selectedRearCameraId = entry.getKey();
                            break;
                        }
                    }
                    selectCamera(CameraSelectionPolicy.LENS_BACK);
                    if (started && previousLens == CameraSelectionPolicy.LENS_BACK
                            && !selectedRearCameraId.equals(previousRearId)) {
                        startPreview();
                    }
                }
            }
        });
        root.addView(cameraGroup);

        TextView fpsLabel = new TextView(this);
        fpsLabel.setText(R.string.output_rate);
        fpsLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        fpsLabel.setPadding(0, dp(12), 0, 0);
        root.addView(fpsLabel);

        fpsGroup = new RadioGroup(this);
        fpsGroup.setOrientation(RadioGroup.HORIZONTAL);
        fpsGroup.setContentDescription(getString(R.string.output_frame_rate_limit));
        fps10Button = fpsOption(10);
        fps15Button = fpsOption(15);
        fps30Button = fpsOption(30);
        fps60Button = fpsOption(60);
        fpsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (syncingControls) {
                return;
            }
            int fps = checkedId == fps10Button.getId() ? 10
                    : checkedId == fps30Button.getId() ? 30
                    : checkedId == fps60Button.getId() ? 60 : 15;
            frameRateLimiter.setMaxFps(fps);
            updateControls();
        });
        root.addView(fpsGroup);

        startButton = new Button(this);
        startButton.setOnClickListener(view -> onStartClicked());
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        startParams.topMargin = dp(12);
        root.addView(startButton, startParams);

        dimScreenButton = new Button(this);
        dimScreenButton.setText(R.string.dim_screen);
        dimScreenButton.setEnabled(false);
        dimScreenButton.setOnClickListener(view -> enterDimScreenMode());
        LinearLayout.LayoutParams dimParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dimParams.topMargin = dp(8);
        root.addView(dimScreenButton, dimParams);

        wifiButton = new Button(this);
        wifiButton.setText(R.string.enable_wifi);
        wifiButton.setEnabled(false);
        wifiButton.setOnClickListener(view -> setWifiEnabled(!wifiEnabled));
        LinearLayout.LayoutParams wifiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wifiParams.topMargin = dp(8);
        root.addView(wifiButton, wifiParams);
        wifiWarningView = new TextView(this);
        wifiWarningView.setText(R.string.wifi_warning);
        wifiWarningView.setVisibility(View.GONE);
        wifiWarningView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        root.addView(wifiWarningView);
        wifiUrlView = new TextView(this);
        wifiUrlView.setVisibility(View.GONE);
        wifiUrlView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        root.addView(wifiUrlView);
        wifiCopyButton = new Button(this);
        wifiCopyButton.setText(R.string.copy_obs_url);
        wifiCopyButton.setContentDescription(getString(R.string.copy_obs_description));
        wifiCopyButton.setVisibility(View.GONE);
        wifiCopyButton.setOnClickListener(view -> copyWifiObsUrl());
        root.addView(wifiCopyButton);

        TextView help = new TextView(this);
        help.setText(R.string.help);
        help.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        root.addView(help);
        FrameLayout content = new FrameLayout(this);
        content.addView(scrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        dimOverlay = new FrameLayout(this);
        dimOverlay.setBackgroundColor(Color.BLACK);
        dimOverlay.setVisibility(View.GONE);
        dimOverlay.setContentDescription(getString(R.string.black_screen_description));
        dimOverlay.setOnClickListener(view -> exitDimScreenMode());
        Button wakeButton = new Button(this);
        wakeButton.setText(R.string.tap_to_restore);
        wakeButton.setTextColor(Color.DKGRAY);
        wakeButton.setBackgroundColor(Color.rgb(8, 8, 8));
        wakeButton.setContentDescription(getString(R.string.restore_description));
        wakeButton.setOnClickListener(view -> exitDimScreenMode());
        FrameLayout.LayoutParams wakeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        dimOverlay.addView(wakeButton, wakeParams);
        content.addView(dimOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(content);
    }

    private RadioButton cameraOption(String label, RadioGroup group) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(label);
        boolean vertical = group.getOrientation() == RadioGroup.VERTICAL;
        group.addView(button, new RadioGroup.LayoutParams(
                CameraOptionLayoutPolicy.width(vertical),
                RadioGroup.LayoutParams.WRAP_CONTENT,
                CameraOptionLayoutPolicy.weight(vertical)));
        return button;
    }

    private void updateCameraOptionLayout(RadioButton button) {
        boolean vertical = cameraGroup.getOrientation() == RadioGroup.VERTICAL;
        button.setLayoutParams(new RadioGroup.LayoutParams(
                CameraOptionLayoutPolicy.width(vertical),
                RadioGroup.LayoutParams.WRAP_CONTENT,
                CameraOptionLayoutPolicy.weight(vertical)));
    }

    private RadioButton fpsOption(int fps) {
        RadioButton button = cameraOption(getString(R.string.fps_option, fps), fpsGroup);
        button.setContentDescription(getString(R.string.fps_description, fps));
        return button;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, getResources().getDisplayMetrics()));
    }

    private void requestCameraProvider() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                populateRearCameras();
                chooseAvailableDefaultCamera();
                updateControls();
                attemptStartPreview();
            } catch (ExecutionException | InterruptedException exception) {
                previewStartGate.cancel();
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                statusView.setText(R.string.camera_unavailable);
                startButton.setEnabled(false);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void chooseAvailableDefaultCamera() {
        lensFacing = CameraSelectionPolicy.defaultLens(
                hasCamera(CameraSelectionPolicy.LENS_BACK),
                hasCamera(CameraSelectionPolicy.LENS_FRONT));
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void populateRearCameras() {
        rearButtons.clear();
        List<String> ids = availableRearIds();
        selectedRearCameraId = CameraSelectionPolicy.selectRearId(selectedRearCameraId, null, ids);
        if (ids.isEmpty()) {
            return;
        }
        cameraGroup.setOrientation(ids.size() > 1 ? RadioGroup.VERTICAL : RadioGroup.HORIZONTAL);
        updateCameraOptionLayout(backButton);
        updateCameraOptionLayout(frontButton);
        rearButtons.put(ids.get(0), backButton);
        for (int index = 1; index < ids.size(); index++) {
            String id = ids.get(index);
            RadioButton button = cameraOption(getString(R.string.rear_camera_numbered, index + 1), cameraGroup);
            rearButtons.put(id, button);
        }
        backButton.setText(ids.size() == 1 ? getString(R.string.rear_camera)
                : getString(R.string.rear_camera_numbered, 1));
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private List<String> availableRearIds() {
        List<String> ids = new ArrayList<>();
        if (cameraProvider == null) {
            return ids;
        }
        for (CameraInfo info : selectorFor(CameraSelectionPolicy.LENS_BACK)
                .filter(cameraProvider.getAvailableCameraInfos())) {
            ids.add(Camera2CameraInfo.from(info).getCameraId());
        }
        return ids;
    }

    private void onStartClicked() {
        if (started) {
            stopPreview();
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startPreview();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void startPreview() {
        previewStartGate.requestStart();
        attemptStartPreview();
    }

    private void attemptStartPreview() {
        if (cameraProvider == null) {
            statusView.setText(R.string.preparing_camera);
            return;
        }
        boolean permissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (!previewStartGate.consumeIfReady(cameraProvider != null, permissionGranted)) {
            return;
        }
        if (!hasCamera(lensFacing)) {
            statusView.setText(R.string.no_camera);
            return;
        }

        long newFrameSession = jpegFrames.beginSession();
        MjpegHttpServer server = mjpegServer;
        if (server == null || !server.isRunning()) {
            server = new MjpegHttpServer(jpegFrames);
            try {
                server.start();
            } catch (IOException exception) {
                jpegFrames.endSession(newFrameSession);
                statusView.setText(R.string.local_stream_failed);
                updateControls();
                return;
            }
        }

        try {
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            ImageAnalysis analysis = new ImageAnalysis.Builder()
                    .setResolutionSelector(new ResolutionSelector.Builder()
                            .setResolutionStrategy(new ResolutionStrategy(new Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                            .build())
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            frameRateLimiter.reset();
            long analyzerSession = newFrameSession;
            analysis.setAnalyzer(analysisExecutor,
                    image -> analyzeFrame(image, analyzerSession));
            cameraProvider.unbindAll();
            Camera boundCamera = cameraProvider.bindToLifecycle(
                    this, selectorFor(lensFacing), preview, analysis);
            boundCameraInfo = boundCamera.getCameraInfo();
            this.boundCamera = boundCamera;
            ZoomState zoomState = boundCameraInfo.getZoomState().getValue();
            zoomMinimum = zoomState == null ? 1f : zoomState.getMinZoomRatio();
            zoomMaximum = zoomState == null ? 1f : zoomState.getMaxZoomRatio();
            float initialZoom = ZoomControlPolicy.initialRatio(zoomMinimum, zoomMaximum);
            boundCamera.getCameraControl().setZoomRatio(initialZoom);
            zoomSeekBar.setProgress(ZoomControlPolicy.progressForRatio(
                    initialZoom, zoomMinimum, zoomMaximum));
            frameSessionId = newFrameSession;
            mjpegServer = server;
            started = true;
            statusView.setText(R.string.camera_running);
            if (wifiEnabled) {
                registerWifiNetworkCallback();
            }
            updateControls();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            started = false;
            boundCamera = null;
            boundCameraInfo = null;
            zoomMinimum = 1f;
            zoomMaximum = 1f;
            jpegFrames.endSession(newFrameSession);
            server.close();
            mjpegServer = null;
            frameSessionId = 0;
            statusView.setText(R.string.preview_failed);
            updateControls();
        }
    }

    private void analyzeFrame(ImageProxy image, long analyzerSession) {
        try {
            long now = System.nanoTime();
            if (!frameRateLimiter.shouldEncode(now)) {
                return;
            }
            byte[] jpeg = YuvJpegEncoder.encode(image);
            if (jpeg != null) {
                jpegFrames.publish(analyzerSession, jpeg);
            }
        } catch (RuntimeException ignored) {
            // Drop malformed or unsupported camera frames without stopping preview.
        } finally {
            image.close();
        }
    }

    private void stopPreview() {
        exitDimScreenMode();
        stopWifiListener(true);
        previewStartGate.cancel();
        started = false;
        jpegFrames.endSession(frameSessionId);
        frameSessionId = 0;
        if (mjpegServer != null) {
            mjpegServer.close();
            mjpegServer = null;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        boundCameraInfo = null;
        boundCamera = null;
        zoomMinimum = 1f;
        zoomMaximum = 1f;
        if (statusView != null) {
            statusView.setText(R.string.camera_stopped);
            updateControls();
        }
    }

    private void selectCamera(int requestedLens) {
        int selected = CameraSelectionPolicy.selectLens(
                lensFacing, requestedLens, hasCamera(requestedLens));
        if (selected != lensFacing) {
            lensFacing = selected;
            if (started) {
                startPreview();
            }
        }
        updateControls();
    }

    private void setWifiEnabled(boolean enabled) {
        if (!started) {
            return;
        }
        wifiEnabled = enabled;
        if (enabled) {
            registerWifiNetworkCallback();
        } else {
            stopWifiListener(false);
        }
        updateControls();
    }

    private void registerWifiNetworkCallback() {
        if (!started || !wifiEnabled || wifiNetworkCallback != null) {
            return;
        }
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        wifiNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (wifiNetworkCallback == this) {
                        wifiNetwork = network;
                        LinkProperties properties = connectivityManager.getLinkProperties(network);
                        updateWifiAddress(properties);
                    }
                });
            }

            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties properties) {
                runOnUiThread(() -> {
                    if (wifiNetworkCallback == this && network.equals(wifiNetwork)) {
                        updateWifiAddress(properties);
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    if (wifiNetworkCallback == this && network.equals(wifiNetwork)) {
                        wifiNetwork = null;
                        updateWifiAddress(null);
                    }
                });
            }
        };
        try {
            connectivityManager.registerNetworkCallback(request, wifiNetworkCallback);
        } catch (RuntimeException exception) {
            wifiNetworkCallback = null;
            wifiNetwork = null;
            updateWifiAddress(null);
        }
    }

    private void updateWifiAddress(LinkProperties properties) {
        List<String> candidates = new ArrayList<>();
        if (properties != null) {
            for (LinkAddress linkAddress : properties.getLinkAddresses()) {
                InetAddress address = linkAddress.getAddress();
                if (address instanceof Inet4Address) {
                    candidates.add(address.getHostAddress());
                }
            }
        }
        String selectedAddress = WifiNetworkPolicy.desiredListenerAddress(
                started, wifiEnabled, candidates);
        if (selectedAddress == null) {
            wifiAddress = null;
            closeWifiServer();
            hideWifiUrl();
            return;
        }
        if (!selectedAddress.equals(wifiAddress) || wifiServer == null || !wifiServer.isRunning()) {
            wifiAddress = selectedAddress;
            startWifiServer(selectedAddress);
        }
        refreshWifiUrl();
    }

    private void startWifiServer(String address) {
        closeWifiServer();
        if (!WifiNetworkPolicy.shouldListen(started, wifiEnabled, address)) {
            hideWifiUrl();
            return;
        }
        if (wifiAuthenticator == null) {
            wifiAuthenticator = MjpegRequestAuthenticator.create();
        }
        try {
            InetAddress bindAddress = InetAddress.getByName(address);
            MjpegHttpServer server = new MjpegHttpServer(jpegFrames, MjpegHttpServer.PORT,
                    wifiAuthenticator, bindAddress);
            server.start();
            wifiServer = server;
        } catch (IOException | SecurityException exception) {
            wifiAddress = null;
            wifiServer = null;
            hideWifiUrl();
        }
    }

    private void refreshWifiUrl() {
        if (!WifiNetworkPolicy.canCopyObsUrl(started, wifiEnabled,
                wifiServer != null && wifiServer.isRunning(), wifiAddress,
                wifiAuthenticator != null)) {
            hideWifiUrl();
            return;
        }
        wifiUrlView.setText(R.string.wifi_ready);
        wifiUrlView.setVisibility(View.VISIBLE);
        wifiCopyButton.setVisibility(View.VISIBLE);
    }

    private void copyWifiObsUrl() {
        if (!WifiNetworkPolicy.canCopyObsUrl(started, wifiEnabled,
                wifiServer != null && wifiServer.isRunning(), wifiAddress,
                wifiAuthenticator != null)) {
            hideWifiUrl();
            return;
        }
        String url = WifiNetworkPolicy.obsUrl(wifiAddress, wifiServer.getLocalPort(),
                wifiAuthenticator.tokenForSession());
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (url == null || clipboard == null) {
            wifiUrlView.setText(R.string.copy_failed);
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clipboard_label), url));
        wifiUrlView.setText(R.string.copy_success);
    }

    private void hideWifiUrl() {
        if (wifiUrlView != null) {
            wifiUrlView.setText("");
            wifiUrlView.setVisibility(View.GONE);
        }
        if (wifiCopyButton != null) {
            wifiCopyButton.setVisibility(View.GONE);
        }
    }

    private void closeWifiServer() {
        if (wifiServer != null) {
            wifiServer.close();
            wifiServer = null;
        }
    }

    private void stopWifiListener(boolean clearOptIn) {
        closeWifiServer();
        if (connectivityManager != null && wifiNetworkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(wifiNetworkCallback);
            } catch (IllegalArgumentException ignored) {
                // The callback may already have been unregistered by the system.
            }
        }
        wifiNetworkCallback = null;
        wifiNetwork = null;
        wifiAddress = null;
        wifiAuthenticator = null;
        if (clearOptIn) {
            wifiEnabled = false;
        }
        hideWifiUrl();
        if (wifiWarningView != null) {
            wifiWarningView.setVisibility(wifiEnabled ? View.VISIBLE : View.GONE);
        }
    }

    private boolean hasCamera(int facing) {
        if (cameraProvider == null) {
            return false;
        }
        try {
            return cameraProvider.hasCamera(selectorFor(facing));
        } catch (CameraInfoUnavailableException exception) {
            return false;
        }
    }

    @NonNull
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private CameraSelector selectorFor(int facing) {
        CameraSelector.Builder builder = new CameraSelector.Builder().requireLensFacing(facing);
        if (facing == CameraSelectionPolicy.LENS_BACK && selectedRearCameraId != null) {
            String id = selectedRearCameraId;
            builder.addCameraFilter(infos -> {
                List<CameraInfo> matches = new ArrayList<>();
                for (CameraInfo info : infos) {
                    if (id.equals(Camera2CameraInfo.from(info).getCameraId())) {
                        matches.add(info);
                    }
                }
                return matches;
            });
        }
        return builder.build();
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void updateControls() {
        if (startButton == null) {
            return;
        }
        startButton.setText(started ? R.string.stop_camera : R.string.start_camera);
        boolean backAvailable = hasCamera(CameraSelectionPolicy.LENS_BACK);
        boolean frontAvailable = hasCamera(CameraSelectionPolicy.LENS_FRONT);
        startButton.setEnabled(backAvailable || frontAvailable);
        dimScreenButton.setEnabled(started && !dimScreenActive);
        boolean zoomSupported = started && boundCamera != null
                && ZoomControlPolicy.supportsZoom(zoomMinimum, zoomMaximum);
        zoomSeekBar.setEnabled(zoomSupported);
        zoomValueView.setText(zoomSupported
                ? getString(R.string.zoom_value, ZoomControlPolicy.ratioForProgress(
                        zoomSeekBar.getProgress(), zoomMinimum, zoomMaximum))
                : getString(R.string.zoom_unavailable));
        themeButton.setEnabled(!started);
        themeButton.setText(themeMode == ThemeModePolicy.DARK ? R.string.theme_dark
                : themeMode == ThemeModePolicy.LIGHT ? R.string.theme_light : R.string.theme_system);
        themeButton.setContentDescription(started ? getString(R.string.theme_while_streaming)
                : themeButton.getText());
        wifiButton.setEnabled(started);
        wifiButton.setText(wifiEnabled ? R.string.disable_wifi : R.string.enable_wifi);
        wifiWarningView.setVisibility(wifiEnabled ? View.VISIBLE : View.GONE);
        syncingControls = true;
        backButton.setEnabled(backAvailable);
        frontButton.setEnabled(frontAvailable);
        RadioButton selectedRear = rearButtons.get(selectedRearCameraId);
        cameraGroup.check(lensFacing == CameraSelectionPolicy.LENS_FRONT
                ? frontButton.getId() : selectedRear == null ? backButton.getId() : selectedRear.getId());
        int advertisedMaximum = selectedCameraAdvertisedMaximum();
        int selectedFps = FpsCapabilityPolicy.reconcileSelection(
                frameRateLimiter.getMaxFps(), advertisedMaximum);
        frameRateLimiter.setMaxFps(selectedFps);
        fps10Button.setEnabled(FpsCapabilityPolicy.isOptionEnabled(10, advertisedMaximum));
        fps15Button.setEnabled(FpsCapabilityPolicy.isOptionEnabled(15, advertisedMaximum));
        fps30Button.setEnabled(FpsCapabilityPolicy.isOptionEnabled(30, advertisedMaximum));
        fps60Button.setEnabled(FpsCapabilityPolicy.isOptionEnabled(60, advertisedMaximum));
        int maxFps = selectedFps;
        fpsGroup.check(maxFps == 10 ? fps10Button.getId()
                : maxFps == 30 ? fps30Button.getId()
                : maxFps == 60 ? fps60Button.getId() : fps15Button.getId());
        syncingControls = false;
        String cameraLabel = lensFacing == CameraSelectionPolicy.LENS_FRONT
                ? getString(R.string.front_camera)
                : selectedRear == null ? getString(R.string.rear_camera)
                : selectedRear.getText().toString();
        selectionView.setText(getString(R.string.selection_summary, cameraLabel, maxFps));
        cameraCapabilityView.setText(advertisedMaximum == FpsCapabilityPolicy.UNKNOWN_MAX_FPS
                ? getString(R.string.capability_unknown)
                : getString(R.string.capability_known, advertisedMaximum));
        if (cameraProvider == null) {
            statusView.setText(R.string.preparing_camera);
        } else if (!backAvailable && !frontAvailable) {
            statusView.setText(R.string.no_camera_device);
        }
    }

    private void enterDimScreenMode() {
        if (!started || dimScreenActive) {
            return;
        }
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        originalScreenBrightness = attributes.screenBrightness;
        attributes.screenBrightness = ScreenDimPolicy.minimumBrightness();
        getWindow().setAttributes(attributes);
        dimScreenActive = true;
        dimOverlay.setVisibility(View.VISIBLE);
        dimOverlay.bringToFront();
        dimScreenButton.setEnabled(false);
    }

    private void exitDimScreenMode() {
        if (!dimScreenActive) {
            return;
        }
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = ScreenDimPolicy.brightnessToRestore(originalScreenBrightness);
        getWindow().setAttributes(attributes);
        originalScreenBrightness = Float.NaN;
        dimScreenActive = false;
        if (dimOverlay != null) {
            dimOverlay.setVisibility(View.GONE);
        }
        if (dimScreenButton != null) {
            dimScreenButton.setEnabled(started);
        }
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private int selectedCameraAdvertisedMaximum() {
        if (cameraProvider == null) {
            return FpsCapabilityPolicy.UNKNOWN_MAX_FPS;
        }
        try {
            CameraInfo selectedInfo = started && boundCameraInfo != null
                    ? boundCameraInfo
                    : selectorFor(lensFacing).filter(cameraProvider.getAvailableCameraInfos()).get(0);
            android.util.Range<Integer>[] ranges = Camera2CameraInfo.from(selectedInfo)
                    .getCameraCharacteristic(
                            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            if (ranges == null || ranges.length == 0) {
                return FpsCapabilityPolicy.UNKNOWN_MAX_FPS;
            }
            int[] upperBounds = new int[ranges.length];
            for (int index = 0; index < ranges.length; index++) {
                upperBounds[index] = ranges[index].getUpper();
            }
            return FpsCapabilityPolicy.advertisedMaximum(upperBounds);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | NullPointerException exception) {
            return FpsCapabilityPolicy.UNKNOWN_MAX_FPS;
        }
    }
}
