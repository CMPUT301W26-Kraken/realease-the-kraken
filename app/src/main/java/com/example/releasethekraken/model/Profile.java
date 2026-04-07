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

    /**
     * Constructs a Profile with name, email, and phone.
     *
     * @param name  The user's full name.
     * @param email The user's email address.
     * @param phone The user's phone number.
     */
    public Profile(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Constructs a Profile with name, email, phone, and profile image URL.
     *
     * @param name            The user's full name.
     * @param email           The user's email address.
     * @param phone           The user's phone number.
     * @param profileImageUrl The URL of the user's profile image.
     */
    public Profile(String name, String email, String phone, String profileImageUrl) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * Gets the user's unique identifier.
     *
     * @return The UID.
     */
    public String getUid() {
        return uid;
    }

    /**
     * Sets the user's unique identifier.
     *
     * @param uid The UID to set.
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * Gets the user's full name.
     *
     * @return The user's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's full name.
     *
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's email address.
     *
     * @return The email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email The email to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's phone number.
     *
     * @return The phone number.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the user's phone number.
     *
     * @param phone The phone number to set.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Gets the URL of the user's profile image.
     *
     * @return The profile image URL.
     */
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    /**
     * Sets the URL of the user's profile image.
     *
     * @param profileImageUrl The profile image URL to set.
     */
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * Gets the user's role as a string.
     *
     * @return The role string.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the user's role string.
     *
     * @param role The role string to set.
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Returns the role as a UserRole enum, defaulting to ENTRANT if unset or unrecognized.
     *
     * @return The corresponding UserRole enum value.
     */
    public UserRole getUserRole() {
        if (role == null || role.trim().isEmpty()) {
            return UserRole.ENTRANT;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UserRole.ENTRANT;
        }
    }
}
