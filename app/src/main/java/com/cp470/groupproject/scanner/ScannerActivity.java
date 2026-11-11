package com.cp470.groupproject.scanner;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import androidx.core.app.ActivityCompat;

import androidx.annotation.NonNull;

import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import androidx.lifecycle.ViewModelProvider;

import android.widget.Button;

import android.text.InputType;
import android.widget.EditText;

import com.cp470.groupproject.R;
import com.cp470.groupproject.util.WindowInsetsHelper;

@androidx.camera.core.ExperimentalGetImage
public class ScannerActivity extends AppCompatActivity {

    /** result extra: the product name found for the scanned barcode */
    public static final String EXTRA_PRODUCT_NAME = "product_name";

    private PreviewView cameraPreview;

    private BarcodeScanner barcodeScanner;

    private BarcodeViewModel barcodeViewModel;

    private boolean barcodeProcessed = false;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scanner);
        WindowInsetsHelper.applySystemBarPadding(findViewById(R.id.root));

        // visible way out: the screen has no toolbar and edge-to-edge hides the system bars' back hint
        findViewById(R.id.backButton).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Set up the manual barcode entry button
        Button manualEntryButton = findViewById(R.id.manualEntryButton);

        manualEntryButton.setOnClickListener(v -> {
            showManualBarcodeDialog();
        });

        cameraPreview = findViewById(R.id.cameraPreview);

        barcodeViewModel = new ViewModelProvider(this)
                .get(BarcodeViewModel.class);

        barcodeViewModel.getProductName().observe(this, name -> {
            if (name != null && !name.isEmpty()) {
                Log.d("ScannerActivity", "Product found: " + name);

                Toast.makeText(
                        this,
                        getString(R.string.scanner_product_found, name),
                        Toast.LENGTH_LONG
                ).show();

                // when opened from the add food form, hand the name back so it can autofill
                if (getCallingActivity() != null) {
                    setResult(RESULT_OK,
                            new android.content.Intent().putExtra(EXTRA_PRODUCT_NAME, name));
                    finish();
                }
            }
        });

        barcodeViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Log.e("ScannerActivity", "Product lookup error: " + getString(error));
                // let the camera read another barcode after a failed lookup
                barcodeProcessed = false;

                Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_EAN_13
                )
                .build();

        barcodeScanner = BarcodeScanning.getClient(options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // Camera permission already granted
            startCamera();

        } else {// Camera permission has not been granted
            showCameraPermissionDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - start camera
                startCamera();

            } else {
                // Permission denied - show a message
                Toast.makeText(
                        this,
                        R.string.scanner_camera_permission_denied,
                        Toast.LENGTH_LONG
                ).show();

            }
        }
    }

    private void showCameraPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.scanner_camera_permission_title)
                .setMessage(R.string.scanner_camera_permission_message)
                .setPositiveButton(R.string.scanner_action_continue, (dialog, which) -> {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST_CODE
                    );
                })
                .setNegativeButton(R.string.scanner_action_cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    @androidx.camera.core.ExperimentalGetImage
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the camera preview
                Preview preview = new Preview.Builder().build();

                // Create an image analyzer for barcode scanning
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        imageProxy -> {
                            processImage(imageProxy);
                        }
                );

                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                // Use the device's back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Remove any previously connected camera use cases
                cameraProvider.unbindAll();

                // Connect the camera to this Activity
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (Exception e) {
                Log.e("ScannerActivity", "Failed to initialize camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImage(ImageProxy imageProxy) {

        android.media.Image mediaImage = imageProxy.getImage();

        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String barcodeValue = barcode.getRawValue();

                        if (barcodeValue != null && !barcodeProcessed) {
                            barcodeProcessed = true;

                            Log.d("ScannerActivity",
                                    "Barcode detected: " + barcodeValue);

                            // Look up the scanned product using the USDA API
                            barcodeViewModel.lookupProduct(barcodeValue);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ScannerActivity",
                            "Barcode scanning failed", e);
                })
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                });
    }

    private void showManualBarcodeDialog() {
        EditText barcodeInput = new EditText(this);
        barcodeInput.setHint(R.string.scanner_manual_hint);
        barcodeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        barcodeInput.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle(R.string.scanner_manual_dialog_title)
                .setView(barcodeInput)
                .setPositiveButton(R.string.scanner_action_search, (dialog, which) -> {
                    String barcode = barcodeInput.getText().toString().trim();

                    if (barcode.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}")) {
                        barcodeProcessed = true;
                        barcodeViewModel.lookupProduct(barcode);
                    } else {
                        Toast.makeText(
                                this,
                                R.string.scanner_error_invalid_barcode_manual,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .setNegativeButton(R.string.scanner_action_cancel, null)
                .show();
    }

}