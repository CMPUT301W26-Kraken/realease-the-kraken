package com.example.releasethekraken.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.example.releasethekraken.model.Profile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository class responsible for storing and retrieving Profile data.
 *
 * Profile data is stored:
 * 1. Locally using SharedPreferences
 * 2. Remotely using Firebase Firestore
 *
 * Firestore document ID is the Firebase Auth UID.
 */
public class ProfileRepository {
    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_UID = "profile_uid"; // Firebase Auth UID: used as Firestore doc ID
    private static final String KEY_NAME = "profile_name";
    private static final String KEY_EMAIL = "profile_email";
    private static final String KEY_PHONE = "profile_phone";
    private static final String KEY_IMAGE_URL = "profile_image_url"; // Firebase Storage download URL

    private static final String COLLECTION_PROFILES = "profiles";
    private static final String STORAGE_PROFILE_IMAGES = "profile_images";

    private final SharedPreferences sharedPreferences;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public ProfileRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public interface ProfileRepositoryCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception exception);
    }

    private FirebaseUser requireCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void saveProfileLocally(Profile profile) {
        sharedPreferences.edit()
                .putString(KEY_UID, profile.getUid())
                .putString(KEY_NAME, profile.getName())
                .putString(KEY_EMAIL, profile.getEmail())
                .putString(KEY_PHONE, profile.getPhone())
                .putString(KEY_IMAGE_URL, profile.getProfileImageUrl())
                .apply();
    }

    /**
     * Saves the profile only to Firestore.
     *
     * Firestore document ID is the Firebase Auth UID.
     *
     * @param profile   The profile to save
     * @param callback  Callback for Firestore success/failure
     */
    public void saveProfileToFirestore(Profile profile, ProfileRepositoryCallback<Void> callback) {
        String documentId = profile.getUid();

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", profile.getUid());
        profileData.put("name", profile.getName());
        profileData.put("email", profile.getEmail());
        profileData.put("phone", profile.getPhone());
        profileData.put("profileImageUrl", profile.getProfileImageUrl());

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

    public void saveProfile(Profile profile, ProfileRepositoryCallback<Void> callback) {
        saveProfileLocally(profile);
        saveProfileToFirestore(profile, callback);
    }

    public Profile getLocalProfile() {
        String uid      = sharedPreferences.getString(KEY_UID, "");
        String name     = sharedPreferences.getString(KEY_NAME, "");
        String email    = sharedPreferences.getString(KEY_EMAIL, "");
        String phone    = sharedPreferences.getString(KEY_PHONE, "");
        String imageUrl = sharedPreferences.getString(KEY_IMAGE_URL, null);
        Profile profile = new Profile(name, email, phone, imageUrl);
        profile.setUid(uid);
        return profile;
    }

    public Profile getProfile() {
        return getLocalProfile();
    }

    /**
     * Retrieves a profile from Firestore using Firebase Auth UID as the document ID.
     *
     * @param uid       Firebase Auth UID used to identify the Firestore document
     * @param callback  Callback for success/failure
     */
    public void getProfileFromFirestore(String uid, ProfileRepositoryCallback<Profile> callback) {
        firestore.collection(COLLECTION_PROFILES)
                .document(uid)
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

    public void getProfileById(String userId, ProfileRepositoryCallback<Profile> callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new Exception("Invalid user ID."));
            return;
        }

        firestore.collection(COLLECTION_PROFILES)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Profile profile = documentSnapshot.toObject(Profile.class);
                        if (profile != null) {
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
     * Searches for a profile by name, email, or phone.
     * This is a simple implementation that matches exactly.
     *
     * @param query    The search string
     * @param callback Callback for success/failure
     */
    public void searchProfiles(String query, ProfileRepositoryCallback<Profile> callback) {
        // Try searching by name first
        firestore.collection(COLLECTION_PROFILES)
                .whereEqualTo("name", query)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(queryDocumentSnapshots.getDocuments().get(0).toObject(Profile.class));
                    } else {
                        // Try email
                        firestore.collection(COLLECTION_PROFILES)
                                .whereEqualTo("email", query)
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    if (!snapshots.isEmpty()) {
                                        callback.onSuccess(snapshots.getDocuments().get(0).toObject(Profile.class));
                                    } else {
                                        // Try phone
                                        firestore.collection(COLLECTION_PROFILES)
                                                .whereEqualTo("phone", query)
                                                .get()
                                                .addOnSuccessListener(phoneSnapshots -> {
                                                    if (!phoneSnapshots.isEmpty()) {
                                                        callback.onSuccess(phoneSnapshots.getDocuments().get(0).toObject(Profile.class));
                                                    } else {
                                                        callback.onFailure(new Exception("User not found"));
                                                    }
                                                })
                                                .addOnFailureListener(callback::onFailure);
                                    }
                                })
                                .addOnFailureListener(callback::onFailure);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates an existing profile both locally and in Firestore.
     *
     * @param profile   The updated profile
     * @param callback  Callback for Firestore success/failure
     */
    public void updateProfile(Profile profile, ProfileRepositoryCallback<Void> callback) {
        saveProfileLocally(profile);

        String documentId = profile.getUid();

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", profile.getUid());
        profileData.put("name", profile.getName());
        profileData.put("email", profile.getEmail());
        profileData.put("phone", profile.getPhone());
        profileData.put("profileImageUrl", profile.getProfileImageUrl());

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

    public boolean hasProfile() {
        return !sharedPreferences.getString(KEY_NAME, "").isEmpty()
                && !sharedPreferences.getString(KEY_EMAIL, "").isEmpty();
    }

    public void deleteLocalProfile() {
        sharedPreferences.edit()
                .remove(KEY_UID)
                .remove(KEY_NAME)
                .remove(KEY_EMAIL)
                .remove(KEY_PHONE)
                .remove(KEY_IMAGE_URL)
                .apply();
    }


    /**
     * Fetches all profiles from Firestore.
     *
     * @param callback Callback returning a list of all profiles on success
     */
    public void getAllProfiles(ProfileRepositoryCallback<List<Profile>> callback) {
        firestore.collection(COLLECTION_PROFILES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Profile> profiles = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Profile profile = doc.toObject(Profile.class);
                        if (profile != null) {
                            if (profile.getUid() == null || profile.getUid().trim().isEmpty()) {
                                profile.setUid(doc.getId());
                            }
                            profiles.add(profile);
                        }
                    }
                    callback.onSuccess(profiles);
                })
                .addOnFailureListener(callback::onFailure);
    }


    /**
     * Deletes the profile from Firestore using Firebase Auth UID as the document ID.
     *
     * @param uid       Firebase Auth UID used to identify the Firestore document
     * @param callback  callback for success/failure
     */
    public void deleteProfileFromFirestore(String uid, ProfileRepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_PROFILES)
                .document(uid)
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

    /**
     * Uploads a profile image to Firebase Storage, then saves the download URL
     * to both Firestore and local SharedPreferences.
     *
     * Storage path: profile_images/{uid}.jpg
     *
     * @param imageUri  Uri of the image selected from the gallery
     * @param uid       Firebase Auth UID of the current user
     * @param callback  Returns the download URL on success
     */
    public void uploadProfileImage(Uri imageUri, String uid, ProfileRepositoryCallback<String> callback) {
        StorageReference ref = storage.getReference()
                .child(STORAGE_PROFILE_IMAGES)
                .child(uid + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String downloadUrl = uri.toString();
                                    // Cache URL locally so ViewProfileFragment doesn't need a Firestore round-trip
                                    sharedPreferences.edit()
                                            .putString(KEY_IMAGE_URL, downloadUrl)
                                            .apply();
                                    // Update the Firestore doc so other devices see the new image
                                    firestore.collection(COLLECTION_PROFILES)
                                            .document(uid)
                                            .update("profileImageUrl", downloadUrl)
                                            .addOnSuccessListener(u -> callback.onSuccess(downloadUrl))
                                            .addOnFailureListener(callback::onFailure);
                                })
                                .addOnFailureListener(callback::onFailure))
                .addOnFailureListener(callback::onFailure);
    }
}