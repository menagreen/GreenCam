package com.example.usbwebcam;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public final class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView statusView;
    private Button startButton;
    private Button switchButton;
    private ProcessCameraProvider cameraProvider;
    private int lensFacing = CameraSelectionPolicy.LENS_BACK;
    private boolean started;

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startPreview();
                } else {
                    statusView.setText("Camera permission is required to start preview.");
                    updateControls();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

    private void createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        root.addView(previewView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        statusView = new TextView(this);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(8, 12, 8, 12);
        root.addView(statusView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        startButton = new Button(this);
        startButton.setOnClickListener(view -> onStartClicked());
        controls.addView(startButton);

        switchButton = new Button(this);
        switchButton.setText("Switch camera");
        switchButton.setOnClickListener(view -> switchCamera());
        controls.addView(switchButton);
        root.addView(controls);
        setContentView(root);
    }

    private void requestCameraProvider() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                chooseAvailableDefaultCamera();
                updateControls();
            } catch (ExecutionException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                statusView.setText("Camera is unavailable on this device.");
                startButton.setEnabled(false);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void chooseAvailableDefaultCamera() {
        if (!hasCamera(lensFacing) && hasCamera(CameraSelectionPolicy.LENS_FRONT)) {
            lensFacing = CameraSelectionPolicy.LENS_FRONT;
        }
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
        if (cameraProvider == null) {
            statusView.setText("Preparing camera…");
            return;
        }
        if (!hasCamera(lensFacing)) {
            statusView.setText("No camera is available.");
            return;
        }

        try {
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, selectorFor(lensFacing), preview);
            started = true;
            statusView.setText("Camera preview is running.");
            updateControls();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            started = false;
            statusView.setText("Unable to start camera preview.");
            updateControls();
        }
    }

    private void stopPreview() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        started = false;
        if (statusView != null) {
            statusView.setText("Camera preview stopped.");
            updateControls();
        }
    }

    private void switchCamera() {
        int nextLens = CameraSelectionPolicy.switchLens(lensFacing);
        if (hasCamera(nextLens)) {
            lensFacing = nextLens;
            if (started) {
                startPreview();
            }
            updateControls();
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
    private CameraSelector selectorFor(int facing) {
        return new CameraSelector.Builder().requireLensFacing(facing).build();
    }

    private void updateControls() {
        if (startButton == null) {
            return;
        }
        startButton.setText(started ? "Stop camera" : "Start camera");
        startButton.setEnabled(cameraProvider != null);
        switchButton.setEnabled(hasCamera(CameraSelectionPolicy.switchLens(lensFacing)));
        if (cameraProvider == null) {
            statusView.setText("Preparing camera…");
        }
    }
}
