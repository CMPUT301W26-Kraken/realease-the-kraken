package com.example.releasethekraken.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.releasethekraken.model.Profile;

/**
 * Repository class responsible for storing and retrieving Profile data.
 *
 * This class acts as a data access layer between the application logic
 * and local storage. It currently uses Android SharedPreferences to
 * persist profile information on the device.
 *
 * SharedPreferences is used here as a temporary storage solution until
 * Firebase Firestore integration is implemented.
 */
public class ProfileRepository {

    // Name of the SharedPreferences file used to store profile data
    private static final String PREFS_NAME = "profile_prefs";

    // Keys used to store individual profile fields
    private static final String KEY_NAME = "profile_name";
    private static final String KEY_EMAIL = "profile_email";
    private static final String KEY_PHONE = "profile_phone";

    // Reference to SharedPreferences instance
    private final SharedPreferences sharedPreferences;


    //Constructor initializes SharedPreferences for profile storage.
    public ProfileRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves a Profile object to local storage.
     *The profile fields are stored individually using SharedPreferences.
     * apply() is used instead of commit() because it performs the operation
     * asynchronously and improves performance.
     */
    public void saveProfile(Profile profile) {
        sharedPreferences.edit()
                .putString(KEY_NAME, profile.getName())
                .putString(KEY_EMAIL, profile.getEmail())
                .putString(KEY_PHONE, profile.getPhone())
                .apply();
    }

    /**
     * Retrieves the stored Profile from local storage.
     * If no profile exists yet, empty strings will be returned for
     * the fields. A new Profile object is created using the stored values.*
     */
    public Profile getProfile() {
        String name = sharedPreferences.getString(KEY_NAME, "");
        String email = sharedPreferences.getString(KEY_EMAIL, "");
        String phone = sharedPreferences.getString(KEY_PHONE, "");

        return new Profile(name, email, phone);
    }


    //Checks whether a profile already exists in storage.
    public boolean hasProfile() {
        return !sharedPreferences.getString(KEY_NAME, "").isEmpty()
                && !sharedPreferences.getString(KEY_EMAIL, "").isEmpty();
    }
}