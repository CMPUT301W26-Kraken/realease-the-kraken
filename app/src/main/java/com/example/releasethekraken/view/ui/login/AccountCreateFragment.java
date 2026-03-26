package com.example.releasethekraken.view.ui.login;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentAccountCreateBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountCreateFragment extends Fragment {

    private FragmentAccountCreateBinding binding;
    private boolean isEditMode = false;

    // Uri of the image the user picked from the gallery (null = no new image chosen)
    private Uri selectedImageUri = null;

    // Launcher for the gallery image picker
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // Preview the chosen image immediately in the button
                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(binding.imageButton);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountCreateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final EditText nameEditText = binding.nameCreate;
        final EditText emailEditText = binding.emailCreate;
        final EditText phoneEditText = binding.phoneCreate;
        final Button saveProfileButton = binding.createAccount;
        final Button cancelAccountButton = binding.cancelAccountCreation;

        ProfileRepository profileRepository = new ProfileRepository(requireContext());

        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean("isEditMode", false);
        }

        if (isEditMode) {
            Profile existingProfile = profileRepository.getProfile();
            nameEditText.setText(existingProfile.getName());
            emailEditText.setText(existingProfile.getEmail());
            phoneEditText.setText(existingProfile.getPhone());

            // Load existing profile picture if one has already been set
            String existingImageUrl = existingProfile.getProfileImageUrl();
            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                Glide.with(this)
                        .load(existingImageUrl)
                        .circleCrop()
                        .placeholder(android.R.drawable.stat_sys_upload_done)
                        .into(binding.imageButton);
            }

            binding.accountCreationWelcome.setText(R.string.action_update_profile);
            saveProfileButton.setText(R.string.action_update_profile);
            cancelAccountButton.setText(R.string.action_cancel_account_edits);
        } else {
            binding.accountCreationWelcome.setText(R.string.action_create_welcome);
            saveProfileButton.setText(R.string.action_create_profile);
            cancelAccountButton.setText(R.string.action_cancel_account_creation);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                emailEditText.setText(currentUser.getEmail());
            } else {
                emailEditText.setText("");
            }

            nameEditText.setText("");
            phoneEditText.setText("");
        }

        // Open gallery when image button is tapped
        binding.imageButton.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*")
        );

        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                clearFieldErrors();
                saveProfileButton.setEnabled(isFormValid(false));
            }
        };

        nameEditText.addTextChangedListener(validationWatcher);
        emailEditText.addTextChangedListener(validationWatcher);
        phoneEditText.addTextChangedListener(validationWatcher);

        saveProfileButton.setEnabled(isFormValid(false));

        saveProfileButton.setOnClickListener(v -> {
            if (!isFormValid(true)) {
                return;
            }

            // Get Firebase Auth UID — replaces the old Device_ID approach
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "";

            Profile profile = new Profile(
                    nameEditText.getText().toString().trim(),
                    emailEditText.getText().toString().trim(),
                    phoneEditText.getText().toString().trim()
            );
            profile.setUid(uid);

            // Keep existing image URL when editing and no new image was picked
            if (isEditMode) {
                String existingUrl = profileRepository.getProfile().getProfileImageUrl();
                profile.setProfileImageUrl(existingUrl);
            }

            if (selectedImageUri != null) {
                // Upload image first, then save profile with the returned URL
                binding.loading.setVisibility(View.VISIBLE);
                saveProfileButton.setEnabled(false);

                profileRepository.uploadProfileImage(selectedImageUri, uid,
                        new ProfileRepository.ProfileRepositoryCallback<String>() {
                            @Override
                            public void onSuccess(String downloadUrl) {
                                profile.setProfileImageUrl(downloadUrl);
                                persistProfile(profile, profileRepository, v, saveProfileButton);
                            }

                            @Override
                            public void onFailure(Exception exception) {
                                if (!isAdded()) return;
                                binding.loading.setVisibility(View.GONE);
                                saveProfileButton.setEnabled(true);
                                Toast.makeText(requireContext(),
                                        "Image upload failed, saving profile without image: " + exception.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                // Still save the profile even if image upload failed
                                persistProfile(profile, profileRepository, v, saveProfileButton);
                            }
                        });
            } else {
                persistProfile(profile, profileRepository, v, saveProfileButton);
            }
        });

        cancelAccountButton.setOnClickListener(v -> {
            if (isEditMode) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_accountCreateFragment_to_viewProfileFragment);
            } else {
                Navigation.findNavController(v)
                        .navigate(R.id.action_accountCreateFragment_to_loginFragment);
            }
        });
    }

    /**
     * Saves the profile locally and syncs to Firestore, then navigates away.
     *
     * @param profile            the profile to persist
     * @param profileRepository  repository handling local and Firestore storage
     * @param navView            view used for navigation
     * @param saveProfileButton  button to re-enable on failure
     */
    private void persistProfile(Profile profile, ProfileRepository profileRepository,
                                View navView, Button saveProfileButton) {
        profileRepository.saveProfileLocally(profile);

        if (isEditMode) {
            Toast.makeText(requireContext(),
                    R.string.profile_updated_message,
                    Toast.LENGTH_SHORT).show();

            profileRepository.updateProfile(profile, new ProfileRepository.ProfileRepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (!isAdded()) return;
                    binding.loading.setVisibility(View.GONE);
                }

                @Override
                public void onFailure(Exception exception) {
                    if (!isAdded()) return;
                    binding.loading.setVisibility(View.GONE);
                    saveProfileButton.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Profile updated locally, but Firestore sync failed: " + exception.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });

            Navigation.findNavController(navView)
                    .navigate(R.id.action_accountCreateFragment_to_viewProfileFragment);

        } else {
            Toast.makeText(requireContext(),
                    R.string.profile_created_message,
                    Toast.LENGTH_SHORT).show();

            profileRepository.saveProfileToFirestore(profile, new ProfileRepository.ProfileRepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (!isAdded()) return;
                    binding.loading.setVisibility(View.GONE);
                }

                @Override
                public void onFailure(Exception exception) {
                    if (!isAdded()) return;
                    binding.loading.setVisibility(View.GONE);
                    saveProfileButton.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Profile was saved locally, but Firestore sync failed: " + exception.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });

            Navigation.findNavController(navView)
                    .navigate(R.id.action_accountCreateFragment_to_mainMenuFragment);
        }
    }

    private boolean isFormValid(boolean showErrors) {
        String name = binding.nameCreate.getText().toString().trim();
        String email = binding.emailCreate.getText().toString().trim();
        String phone = binding.phoneCreate.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            if (showErrors) binding.nameCreate.setError(getString(R.string.error_name_required));
            isValid = false;
        }

        if (email.isEmpty()) {
            if (showErrors) binding.emailCreate.setError(getString(R.string.error_email_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (showErrors) binding.emailCreate.setError(getString(R.string.error_email_invalid));
            isValid = false;
        }

        if (!phone.isEmpty() && phone.length() < 7) {
            if (showErrors) binding.phoneCreate.setError(getString(R.string.error_phone_invalid));
            isValid = false;
        }

        return isValid;
    }

    private void clearFieldErrors() {
        binding.nameCreate.setError(null);
        binding.emailCreate.setError(null);
        binding.phoneCreate.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}