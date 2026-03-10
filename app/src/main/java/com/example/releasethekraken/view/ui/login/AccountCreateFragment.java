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

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentAccountCreateBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;

/**
 * Fragment responsible for creating or updating a user profile.
 *
 * If a profile already exists, the form is pre-filled and the screen behaves
 * as an update-profile screen. Otherwise, it behaves as a create-profile screen.
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

        // If a profile already exists, switch this screen into edit mode
        if (profileRepository.hasProfile()) {
            isEditMode = true;

            Profile existingProfile = profileRepository.getProfile();
            nameEditText.setText(existingProfile.getName());
            emailEditText.setText(existingProfile.getEmail());
            phoneEditText.setText(existingProfile.getPhone());

            binding.accountCreationWelcome.setText(R.string.action_update_profile);
            saveProfileButton.setText(R.string.action_update_profile);
        }

        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed while text is changing
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

        // Enable button immediately if prefilled data is already valid
        saveProfileButton.setEnabled(isFormValid(false));

        saveProfileButton.setOnClickListener(v -> {
            if (!isFormValid(true)) {
                return;
            }

            Profile profile = new Profile(
                    nameEditText.getText().toString().trim(),
                    emailEditText.getText().toString().trim(),
                    phoneEditText.getText().toString().trim()
            );

            profileRepository.saveProfile(profile);

            int messageResId = isEditMode
                    ? R.string.profile_updated_message
                    : R.string.profile_created_message;

            Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show();

            if (isEditMode) {
                Navigation.findNavController(view)
                        .navigate(R.id.action_accountCreateFragment_to_viewProfileFragment);
            } else {
                Navigation.findNavController(view)
                        .navigate(R.id.action_accountCreateFragment_to_mainMenuFragment);
            }
        });

        cancelAccountButton.setOnClickListener(v -> {
            if (isEditMode) {
                Navigation.findNavController(view)
                        .navigate(R.id.action_accountCreateFragment_to_viewProfileFragment);
            } else {
                Navigation.findNavController(view)
                        .navigate(R.id.action_accountCreateFragment_to_loginFragment);
            }
        });
    }

    /**
     * Validates the profile form fields.
     *
     * @param showErrors true if field errors should be displayed to the user
     * @return true if all required fields are valid, false otherwise
     */
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

    /**
     * Clears all current validation errors from the form fields.
     */
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