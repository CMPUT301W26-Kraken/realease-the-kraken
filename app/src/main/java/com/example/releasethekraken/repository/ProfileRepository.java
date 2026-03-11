package com.example.releasethekraken.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.releasethekraken.model.Profile;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository class responsible for storing and retrieving Profile data.
 *
 * Profile data is currently stored in two places:
 * 1. Locally using SharedPreferences
 * 2. Remotely using Firebase Firestore
 *
 * Local storage is used as a fallback while Firebase integration is being completed.
 */
public class ProfileRepository {
    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_NAME = "profile_name";
    private static final String KEY_EMAIL = "profile_email";
    private static final String KEY_PHONE = "profile_phone";

    private static final String COLLECTION_PROFILES = "profiles";

    private final SharedPreferences sharedPreferences;
    private final FirebaseFirestore firestore;

    public ProfileRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Callback interface for asynchronous Firestore operations.
     */
    public interface ProfileRepositoryCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception exception);
    }

    /**
     * Saves the profile locally in SharedPreferences.
     *
     * @param profile The profile to save locally
     */
    public void saveProfileLocally(Profile profile) {
        sharedPreferences.edit()
                .putString(KEY_NAME, profile.getName())
                .putString(KEY_EMAIL, profile.getEmail())
                .putString(KEY_PHONE, profile.getPhone())
                .apply();
    }

    /**
     * Saves the profile only to Firestore.
     *
     * Firestore document ID is the normalized email address.
     *
     * @param profile   The profile to save
     * @param callback  Callback for Firestore success/failure
     */
    public void saveProfileToFirestore(Profile profile, ProfileRepositoryCallback<Void> callback) {
        String documentId = normalizeEmail(profile.getEmail());

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", profile.getName());
        profileData.put("email", profile.getEmail());
        profileData.put("phone", profile.getPhone());

        Log.d("ProfileRepository", "Saving profile to Firestore with doc id: " + documentId);

        firestore.collection(COLLECTION_PROFILES)
                .document(documentId)
                .set(profileData)
                .addOnSuccessListener(unused -> {
                    Log.d("ProfileRepository", "Firestore save success");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileRepository", "Firestore save failed", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Backward-compatible method that saves both locally and to Firestore.
     *
     * @param profile   The profile to save
     * @param callback  Callback for Firestore success/failure
     */
    public void saveProfile(Profile profile, ProfileRepositoryCallback<Void> callback) {
        saveProfileLocally(profile);
        saveProfileToFirestore(profile, callback);
    }

    /**
     * Returns the locally stored profile.
     *
     * @return locally stored Profile object
     */
    public Profile getLocalProfile() {
        String name = sharedPreferences.getString(KEY_NAME, "");
        String email = sharedPreferences.getString(KEY_EMAIL, "");
        String phone = sharedPreferences.getString(KEY_PHONE, "");
        return new Profile(name, email, phone);
    }

    /**
     * Backward-compatible method for existing code that expects getProfile().
     *
     * @return locally stored Profile object
     */
    public Profile getProfile() {
        return getLocalProfile();
    }

    /**
     * Retrieves a profile from Firestore using email as the document ID.
     *
     * @param email     Email used to identify the Firestore document
     * @param callback  Callback for success/failure
     */
    public void getProfileFromFirestore(String email, ProfileRepositoryCallback<Profile> callback) {
        String documentId = normalizeEmail(email);

        firestore.collection(COLLECTION_PROFILES)
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Profile profile = documentSnapshot.toObject(Profile.class);
                        if (profile != null) {
                            saveProfileLocally(profile);
                            callback.onSuccess(profile);
                        } else {
                            callback.onFailure(new Exception("Profile data was empty."));
                        }
                    } else {
                        callback.onFailure(new Exception("Profile does not exist."));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Checks whether a profile exists locally.
     *
     * @return true if required local profile fields are present
     */
    public boolean hasProfile() {
        return !sharedPreferences.getString(KEY_NAME, "").isEmpty()
                && !sharedPreferences.getString(KEY_EMAIL, "").isEmpty();
    }

    /**
     * Normalizes email for safe and consistent Firestore document IDs.
     *
     * @param email input email
     * @return normalized email
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}