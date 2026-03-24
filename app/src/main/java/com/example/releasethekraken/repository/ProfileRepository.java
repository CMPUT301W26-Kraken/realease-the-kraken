package com.example.releasethekraken.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.example.releasethekraken.model.Profile;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
    private static final String KEY_DEVICE_ID = "profile_device_id"; // ANDROID_ID: this is stored locally, used as Firestore doc ID
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
                .putString(KEY_DEVICE_ID, profile.getDeviceId())
                .putString(KEY_NAME, profile.getName())
                .putString(KEY_EMAIL, profile.getEmail())
                .putString(KEY_PHONE, profile.getPhone())
                .apply();
    }

    /**
     * Saves the profile only to Firestore.
     *
     * Firestore document ID is the device ID (ANDROID_ID).
     * To add login: swap profile.getDeviceId() with FirebaseAuth.getInstance().getCurrentUser().getUid()
     *
     * @param profile   The profile to save
     * @param callback  Callback for Firestore success/failure
     */
    public void saveProfileToFirestore(Profile profile, ProfileRepositoryCallback<Void> callback) {
        String documentId = profile.getDeviceId(); // doc ID = ANDROID_ID, swap to UID when login is added

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("deviceId", profile.getDeviceId());
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
        String deviceId = sharedPreferences.getString(KEY_DEVICE_ID, "");
        String name = sharedPreferences.getString(KEY_NAME, "");
        String email = sharedPreferences.getString(KEY_EMAIL, "");
        String phone = sharedPreferences.getString(KEY_PHONE, "");
        return new Profile(deviceId, name, email, phone);
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
     * Retrieves a profile from Firestore using device ID as the document ID.
     *
     * @param deviceId  Device ID used to identify the Firestore document
     * @param callback  Callback for success/failure
     */
    public void getProfileFromFirestore(String deviceId, ProfileRepositoryCallback<Profile> callback) {
        firestore.collection(COLLECTION_PROFILES)
                .document(deviceId)
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

    public void updateProfile(Profile profile, ProfileRepositoryCallback<Void> callback) {

        // Save locally first
        saveProfileLocally(profile);

        String documentId = profile.getDeviceId(); // doc ID = ANDROID_ID, swap to UID when login is added

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("deviceId", profile.getDeviceId());
        profileData.put("name", profile.getName());
        profileData.put("email", profile.getEmail());
        profileData.put("phone", profile.getPhone());

        firestore.collection(COLLECTION_PROFILES)
                .document(documentId)
                .set(profileData)
                .addOnSuccessListener(unused -> {
                    Log.d("ProfileRepository", "Profile updated in Firestore");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileRepository", "Profile update failed", e);
                    callback.onFailure(e);
                });
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

    /**
     * Deletes the locally stored profile from SharedPreferences.
     */
    public void deleteLocalProfile() {
        sharedPreferences.edit()
                .remove(KEY_DEVICE_ID)
                .remove(KEY_NAME)
                .remove(KEY_EMAIL)
                .remove(KEY_PHONE)
                .apply();
    }

    /**
     * Deletes the profile from Firestore using device ID as the document ID.
     *
     * @param deviceId  device ID used to identify the Firestore document
     * @param callback  callback for success/failure
     */
    public void deleteProfileFromFirestore(String deviceId, ProfileRepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_PROFILES)
                .document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d("ProfileRepository", "Profile deleted from Firestore");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileRepository", "Profile delete failed", e);
                    callback.onFailure(e);
                });
    }

    // image storing with Glide:
    // Root reference to the Firebase Storage bucket
    private final StorageReference storageRef =
            FirebaseStorage.getInstance().getReference();

    /**
     * Uploads a profile image to Firebase Storage and returns the HTTPS download URL.
     *
     * Storage path: profile_images/{documentId}.jpg
     * Using documentId as the filename means re-uploading overwrites the old photo —
     * no orphaned files accumulate in Storage.
     *
     * After getting the URL, call saveProfileImageUrl() to persist it to Firestore.
     *
     * @param documentId  Firestore document ID for this profile (deviceId or Firebase UID)
     * @param imageUri    local URI of the image chosen by the user from gallery
     * @param callback    called with the HTTPS download URL on success, exception on failure
     */
    public void uploadProfileImage(String documentId, Uri imageUri,
                                   ProfileRepositoryCallback<String> callback) {
        // profile_images/{documentId}.jpg — overwrites on re-upload, no duplicates
        StorageReference imageRef = storageRef.child("profile_images/" + documentId + ".jpg");

        // putFile() streams the local file up to Firebase Storage
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Upload succeeded — get the permanent HTTPS download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        Log.d("ProfileRepository", "Image uploaded: " + downloadUri);
                        callback.onSuccess(downloadUri.toString()); // return URL string to caller
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileRepository", "Image upload failed", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Saves the profile image URL to the existing Firestore profile document.
     * Uses update() so only the profileImageUrl field changes — all other fields untouched.
     *
     * @param documentId  Firestore document ID for this profile
     * @param imageUrl    HTTPS download URL returned by uploadProfileImage()
     * @param callback    called on success or failure
     */
    public void saveProfileImageUrl(String documentId, String imageUrl,
                                    ProfileRepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_PROFILES)
                .document(documentId)
                .update("profileImageUrl", imageUrl) // update() only touches this one field
                .addOnSuccessListener(unused -> {
                    Log.d("ProfileRepository", "Image URL saved to Firestore");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileRepository", "Failed to save image URL", e);
                    callback.onFailure(e);
                });
    }
}