package com.example.releasethekraken.controller;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.releasethekraken.model.UserRole;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SessionManager {
    private static final String PREFS_NAME = "kraken_session";
    private static final String KEY_ROLE = "active_role";
    private static final String KEY_UID = "firebase_uid"; // Firebase Auth UID cached locally

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Returns the Firebase Auth UID for the current user.
    // Falls back to the locally cached UID if Firebase is temporarily unavailable.
    public String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Always prefer the live UID and keep the cache in sync
            preferences.edit().putString(KEY_UID, user.getUid()).apply();
            return user.getUid();
        }
        // Fallback to cached UID (e.g. offline startup before auth resolves)
        return preferences.getString(KEY_UID, null);
    }

    // Called from MainActivity after anonymous sign-in succeeds.
    // Caches the UID locally so getCurrentUserId() works even before Firebase resolves.
    public void setUid(String uid) {
        preferences.edit().putString(KEY_UID, uid).apply();
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