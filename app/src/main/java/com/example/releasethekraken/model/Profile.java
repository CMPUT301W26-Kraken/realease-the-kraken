package com.example.releasethekraken.model;

/**
 * Profile model class representing a user's personal information in the app.
 * This class is used for both local storage and Firebase Firestore.
 * Firestore requires a public no-argument constructor and non-final fields
 * so that it can automatically deserialize documents into Profile objects.
 */
public class Profile {

    // Firebase Auth UID — used as the Firestore document ID (profiles/{uid}).
    private String uid;

    // User's full name (required field)
    private String name;

    // User's email address (required field)
    private String email;

    // User's phone number (optional field)
    private String phone;

    // Firebase Storage download URL for the user's profile picture.
    // Null/empty means no custom picture has been set yet — UI falls back to default drawable.
    private String profileImageUrl;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Profile() {
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

    /**
     * Returns the user's full name.
     *
     * @return profile name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the user's email address.
     *
     * @return profile email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the user's phone number.
     *
     * @return profile phone number
     */
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