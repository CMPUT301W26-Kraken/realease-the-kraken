package com.example.releasethekraken.model;

/**
 * Profile model for a signed-in user.
 *
 * Firestore requires a public no-argument constructor and setters for deserialization.
 */
public class Profile {

    // Firebase Auth UID — used as the Firestore document ID (profiles/{uid}).
    private String uid;

    // User's full name
    private String name;
    private String email;
    private String phone;

    // Firebase Storage download URL for the user's profile picture.
    private String profileImageUrl;

    // User's current role stored as a string (e.g. "ENTRANT", "ORGANIZER", "ADMIN").
    // Defaults to ENTRANT for backward compatibility with existing Firestore documents.
    private String role;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Profile() {
        // Required for Firestore
    }

    public Profile(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public Profile(String name, String email, String phone, String profileImageUrl) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Returns the role as a UserRole enum, defaulting to ENTRANT if unset or unrecognized.
     */
    public UserRole getUserRole() {
        if (role == null || role.trim().isEmpty()) {
            return UserRole.ENTRANT;
        }
        try {
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return UserRole.ENTRANT;
        }
    }
}