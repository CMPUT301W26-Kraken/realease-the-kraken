package com.example.releasethekraken;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.releasethekraken.controller.SessionManager;
import com.example.releasethekraken.util.AccessibilitySettingsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser existing = auth.getCurrentUser();

        if (existing != null) {
            new SessionManager(this).setUid(existing.getUid());
            Log.d("AUTH", "User already signed in, UID: " + existing.getUid());
        } else {
            Log.d("AUTH", "No user signed in yet.");
        }

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

        getWindow().getDecorView().post(() ->
                AccessibilitySettingsHelper.applyAccessibility(getWindow().getDecorView(), this)
        );

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                    getWindow().getDecorView().post(() ->
                            AccessibilitySettingsHelper.applyAccessibility(getWindow().getDecorView(), this)
                    )
            );
        }
    }
}