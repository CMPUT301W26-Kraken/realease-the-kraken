package com.example.releasethekraken.view.ui.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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
import android.provider.Settings;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentAccountCreateBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;

/**
 * Fragment responsible for creating or updating a user profile.
 *
 * Mode is determined by navigation argument:
 * - false -> create mode
 * - true  -> update mode
 */
public class AccountCreateFragment extends Fragment {

    private FragmentAccountCreateBinding binding;
    private boolean isEditMode = false;

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

        // Only prefill fields when explicitly opened in edit mode
        if (isEditMode) {
            Profile existingProfile = profileRepository.getProfile();
            nameEditText.setText(existingProfile.getName());
            emailEditText.setText(existingProfile.getEmail());
            phoneEditText.setText(existingProfile.getPhone());

            binding.accountCreationWelcome.setText(R.string.action_update_profile);
            saveProfileButton.setText(R.string.action_update_profile);
            cancelAccountButton.setText(R.string.action_cancel_account_edits);
        } else {
            binding.accountCreationWelcome.setText(R.string.action_create_welcome);
            saveProfileButton.setText(R.string.action_create_profile);

            // Clear fields in create mode
            nameEditText.setText("");
            emailEditText.setText("");
            phoneEditText.setText("");
        }

        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

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

            //Added to fix crash. Gets device id and constructs profile with it
            String deviceId = Settings.Secure.getString(
                    requireContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            Profile profile = new Profile(
                    deviceId,
                    nameEditText.getText().toString().trim(),
                    emailEditText.getText().toString().trim(),
                    phoneEditText.getText().toString().trim()
            );

            if (isEditMode) {
                profileRepository.saveProfileLocally(profile);

                Toast.makeText(requireContext(),
                        R.string.profile_updated_message,
                        Toast.LENGTH_SHORT).show();

                profileRepository.updateProfile(profile, new ProfileRepository.ProfileRepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(requireContext(),
                                "Profile updated locally, but Firestore sync failed: " + exception.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

                Navigation.findNavController(v)
                        .navigate(R.id.action_accountCreateFragment_to_viewProfileFragment);

            } else {
                profileRepository.saveProfileLocally(profile);

                Toast.makeText(requireContext(),
                        R.string.profile_created_message,
                        Toast.LENGTH_SHORT).show();

                profileRepository.saveProfileToFirestore(profile, new ProfileRepository.ProfileRepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(requireContext(),
                                "Profile was saved locally, but Firestore sync failed: " + exception.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

                Navigation.findNavController(v)
                        .navigate(R.id.action_accountCreateFragment_to_mainMenuFragment);
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

    private boolean isFormValid(boolean showErrors) {
        String name = binding.nameCreate.getText().toString().trim();
        String email = binding.emailCreate.getText().toString().trim();
        String phone = binding.phoneCreate.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            if (showErrors) {
                binding.nameCreate.setError(getString(R.string.error_name_required));
            }
            isValid = false;
        }

        if (email.isEmpty()) {
            if (showErrors) {
                binding.emailCreate.setError(getString(R.string.error_email_required));
            }
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (showErrors) {
                binding.emailCreate.setError(getString(R.string.error_email_invalid));
            }
            isValid = false;
        }

        if (!phone.isEmpty() && phone.length() < 7) {
            if (showErrors) {
                binding.phoneCreate.setError(getString(R.string.error_phone_invalid));
            }
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