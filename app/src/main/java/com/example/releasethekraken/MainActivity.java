package com.example.releasethekraken;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.releasethekraken.controller.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sign in anonymously so every install gets a stable Firebase Auth UID.
        // If already signed in (app restart), this is a no-op and the existing UID is reused.
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser existing = auth.getCurrentUser();

        if (existing != null) {
            // Already signed in — cache UID and continue
            new SessionManager(this).setUid(existing.getUid());
            Log.d("AUTH", "Already signed in, UID: " + existing.getUid());
        } else {
            auth.signInAnonymously().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    String uid = auth.getCurrentUser().getUid();
                    new SessionManager(this).setUid(uid);
                    Log.d("AUTH", "Anonymous sign-in success, UID: " + uid);
                } else {
                    Log.e("AUTH", "Anonymous sign-in failed", task.getException());
                }
            });
        }

        // Fetches the FCM push notification token and logs it to verify Firebase Messaging is working
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.d("FCM", "Token: " + token);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}