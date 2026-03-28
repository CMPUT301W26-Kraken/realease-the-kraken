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
    public String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Sync cache
            setUid(user.getUid());
            return user.getUid();
        }
        return preferences.getString(KEY_UID, null);
    }

    public void setUid(String uid) {
        preferences.edit().putString(KEY_UID, uid).apply();
    }

    public void clearSession() {
        preferences.edit().remove(KEY_UID).remove(KEY_ROLE).apply();
    }

    public UserRole getRole() {
        String storedRole = preferences.getString(KEY_ROLE, UserRole.ENTRANT.name());
        try {
            return UserRole.valueOf(storedRole);
        } catch (IllegalArgumentException ex) {
            return UserRole.ENTRANT;
        }
    }

    public void setRole(UserRole role) {
        preferences.edit().putString(KEY_ROLE, role.name()).apply();
    }
}