package com.example.releasethekraken.model;

/**
 * Profile model for a signed-in user.
 *
 * Firestore requires a public no-argument constructor.
 */
public class Profile {

    // Firebase Auth UID — used as the Firestore document ID (profiles/{uid}).
    private String uid;

    // User's full name (required field)
    private String name;
    private String email;
    private String phone;

    // Firebase Storage download URL for the user's profile picture.
    // Null/empty means no custom picture has been set yet — UI falls back to default drawable.
    private String profileImageUrl;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Profile() {
        // Required for Firestore
    }

    /**
     * Constructs a new Profile object. UID is set separately via Firebase Auth.
     *
     * @param name  the user's full name
     * @param email the user's email address
     * @param phone the user's phone number (can be empty)
     */
    public Profile(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Constructs a new Profile object with profile image. UID is set separately via Firebase Auth.
     *
     * @param name            the user's full name
     * @param email           the user's email address
     * @param phone           the user's phone number (can be empty)
     * @param profileImageUrl Firebase Storage download URL for the profile picture
     */
    public Profile(String name, String email, String phone, String profileImageUrl) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * Returns the Firebase Auth UID used as the Firestore document ID.
     *
     * @return Firebase Auth UID
     */
    public String getUid() {
        return uid;
    }

    /**
     * Sets the Firebase Auth UID. Called after Firebase Auth sign-in resolves.
     *
     * @param uid Firebase Auth UID
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    /**
     * Returns the Firebase Storage download URL for the profile picture.
     *
     * @return profileImageUrl, or null if no picture has been uploaded
     */
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    /**
     * Sets the Firebase Storage download URL for the profile picture.
     *
     * @param profileImageUrl download URL from Firebase Storage
     */
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}