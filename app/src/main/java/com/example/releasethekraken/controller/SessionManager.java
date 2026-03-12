package com.example.releasethekraken.controller;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.releasethekraken.model.UserRole;

public class SessionManager {
    private static final String PREFS_NAME = "kraken_session";
    private static final String KEY_ROLE = "active_role";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
