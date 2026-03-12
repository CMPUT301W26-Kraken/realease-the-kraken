package com.example.releasethekraken;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.releasethekraken.controller.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Grab the hardware-level device ID (unique per device+app install) and store it in SessionManager.
        // All identity lookups go through SessionManager.getCurrentUserId(), not directly here.
        @SuppressLint("HardwareIds") String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        new SessionManager(this).setDeviceId(deviceId);

        // TEST logs stored in LOGCAT to confirm SessionManager is working - COMMENT OUT IF NEEDED
        String storedId = new SessionManager(this).getCurrentUserId();
        Log.d("AUTH_TEST", "Device ID set: " + deviceId);
        Log.d("AUTH_TEST", "SessionManager reads back: " + storedId);
        Log.d("AUTH_TEST", "Match: " + deviceId.equals(storedId));

        // Fetches the FCM push notification token and logs it to verify Firebase Messaging is working
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.d("FCM", "Token: " + token);
            }
        });

        // Confirms Firebase Storage bucket is reachable — remove once Firebase Storage is successfully integrated
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        Log.d("StorageTest", "Storage connected: " + storageRef.getBucket());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
