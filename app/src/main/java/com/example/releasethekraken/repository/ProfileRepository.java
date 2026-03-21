package com.example.releasethekraken.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.releasethekraken.model.Profile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private static final String KEY_USER_ID = "profile_user_id";
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

    public interface ProfileRepositoryCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception exception);
    }

    private FirebaseUser requireCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void saveProfileLocally(Profile profile) {
        sharedPreferences.edit()
                .putString(KEY_USER_ID, profile.getUserId())
                .putString(KEY_NAME, profile.getName())
                .putString(KEY_EMAIL, profile.getEmail())
                .putString(KEY_PHONE, profile.getPhone())
                .apply();
    }

    public void saveProfileToFirestore(Profile profile, ProfileRepositoryCallback<Void> callback) {
        FirebaseUser currentUser = requireCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("No authenticated user found."));
            return;
        }

        String documentId = currentUser.getUid();

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("userId", profile.getUserId());
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

    public void saveProfile(Profile profile, ProfileRepositoryCallback<Void> callback) {
        saveProfileLocally(profile);
        saveProfileToFirestore(profile, callback);
    }

    public Profile getLocalProfile() {
        String userId = sharedPreferences.getString(KEY_USER_ID, "");
        String name = sharedPreferences.getString(KEY_NAME, "");
        String email = sharedPreferences.getString(KEY_EMAIL, "");
        String phone = sharedPreferences.getString(KEY_PHONE, "");
        return new Profile(userId, name, email, phone);
    }

    public Profile getProfile() {
        return getLocalProfile();
    }

    public void getProfileFromFirestore(ProfileRepositoryCallback<Profile> callback) {
        FirebaseUser currentUser = requireCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("No authenticated user found."));
            return;
        }

        String documentId = currentUser.getUid();

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

    public void updateProfile(Profile profile, ProfileRepositoryCallback<Void> callback) {
        saveProfileLocally(profile);
        saveProfileToFirestore(profile, callback);
    }

    public boolean hasProfile() {
        return !sharedPreferences.getString(KEY_NAME, "").isEmpty()
                && !sharedPreferences.getString(KEY_EMAIL, "").isEmpty();
    }

    public void deleteLocalProfile() {
        sharedPreferences.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_NAME)
                .remove(KEY_EMAIL)
                .remove(KEY_PHONE)
                .apply();
    }

    public void deleteProfileFromFirestore(ProfileRepositoryCallback<Void> callback) {
        FirebaseUser currentUser = requireCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("No authenticated user found."));
            return;
        }

        String documentId = currentUser.getUid();

        firestore.collection(COLLECTION_PROFILES)
                .document(documentId)
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
}