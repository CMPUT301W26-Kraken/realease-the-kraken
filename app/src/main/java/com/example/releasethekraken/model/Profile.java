package com.example.releasethekraken.model;

/**
 * Profile model class representing a user's personal information in the app.
 * This class is used for both local storage and Firebase Firestore.
 * Firestore requires a public no-argument constructor and non-final fields
 * so that it can automatically deserialize documents into Profile objects.
 */
public class Profile {

    // Unique device identifier — Firestore document ID (profiles/{deviceId}).
    // When login is added, stays as a field but the doc ID switches to Firebase Auth UID.
    private String deviceId;

    // User's full name (required field)
    private String name;

    // User's email address (required field)
    private String email;

    // User's phone number (optional field)
    private String phone;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Profile() {
    }

    /**
     * Constructs a new Profile object.
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
     * Constructs a new Profile object with device identity.
     *
     * @param deviceId the device's ANDROID_ID, used as the Firestore document ID
     * @param name  the user's full name
     * @param email the user's email address
     * @param phone the user's phone number (can be empty)
     */
    public Profile(String deviceId, String name, String email, String phone) {
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Returns the device ID used as the Firestore document ID.
     *
     * @return deviceId (ANDROID_ID)
     */
    public String getDeviceId() {
        return deviceId;
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
}