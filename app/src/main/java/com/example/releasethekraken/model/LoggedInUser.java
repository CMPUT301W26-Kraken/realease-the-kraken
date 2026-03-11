package com.example.releasethekraken.model;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 * Default Class created by Android Studio
 */
public class LoggedInUser {

    private String userId;
    private String displayName;

    public LoggedInUser(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }
}