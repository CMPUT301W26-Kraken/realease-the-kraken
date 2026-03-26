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
}