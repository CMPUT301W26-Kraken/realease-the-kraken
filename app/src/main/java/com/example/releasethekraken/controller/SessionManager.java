package com.example.releasethekraken.controller;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.releasethekraken.model.UserRole;

public class SessionManager {
    private static final String PREFS_NAME = "kraken_session";
    private static final String KEY_ROLE = "active_role";
    private static final String KEY_DEVICE_ID = "device_id"; // Unique device ID to track user ANDROID_ID across app restarts

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Returns the active user ID used as the Firestore document ID in profiles/{id}.
    // Currently: returns ANDROID_ID set on launch. To add login: replace with FirebaseAuth.getInstance().getCurrentUser().getUid()
    public String getCurrentUserId() {
        return preferences.getString(KEY_DEVICE_ID, null);
    }

    // Called once in MainActivity.onCreate() to store ANDROID_ID into SharedPreferences
    public void setDeviceId(String deviceId) {
        preferences.edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }

    // Reads role from SharedPreferences, defaults to ENTRANT if missing or corrupted
    public UserRole getRole() {
        String storedRole = preferences.getString(KEY_ROLE, UserRole.ENTRANT.name());
        try {
            return UserRole.valueOf(storedRole);
        } catch (IllegalArgumentException ex) {
            return UserRole.ENTRANT;
        }
    }

    // Persists role selection across app restarts via SharedPreferences
    public void setRole(UserRole role) {
        preferences.edit().putString(KEY_ROLE, role.name()).apply();
    }
}