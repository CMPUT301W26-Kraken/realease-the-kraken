package com.example.releasethekraken.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.R;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.EventRepository;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

public class QRScanFragment extends Fragment {

    private static final String TAG = "QRScanFragment";
    private PreviewView previewView;
    private boolean scanned = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scan, container, false);
        previewView = view.findViewById(R.id.previewView);

        // For testing in emulator without a real QR:
        // Long press will find the MOST RECENTLY CREATED event from the database
        view.setOnLongClickListener(v -> {
            Toast.makeText(getContext(), "Simulating Scan...", Toast.LENGTH_SHORT).show();
            
            new EventRepository().getMostRecentEvent(new EventRepository.EventCallback() {
                @Override
                public void onSuccess(com.example.releasethekraken.model.Event event) {
                    if (event != null) {
                        openEventDetails(event.getEventId());
                    } else {
                        Toast.makeText(getContext(), "No events found!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error getting recent event", e);
                    Toast.makeText(getContext(), "Scan simulation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), this::processImage);
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            } catch (Exception e) { Log.e(TAG, "Camera error", e); }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void processImage(ImageProxy imageProxy) {
        if (scanned) { imageProxy.close(); return; }
        @android.annotation.SuppressLint("UnsafeOptInUsageError")
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            BarcodeScanning.getClient().process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null) {
                                scanned = true;
                                String id = rawValue.startsWith("event:") ? rawValue.split(":")[1] : rawValue;
                                openEventDetails(id);
                                break;
                            }
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> imageProxy.close());
        } else { imageProxy.close(); }
    }

    private void openEventDetails(String eventId) {
        Bundle bundle = new Bundle();
        bundle.putString("eventId", eventId);
        bundle.putSerializable("UserType", UserRole.ENTRANT);
        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.action_qrScanFragment_to_eventDetailsFragment, bundle);
        }
    }
}
