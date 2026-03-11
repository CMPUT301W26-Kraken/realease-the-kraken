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
 * Fragment responsible for creating a user profile.
 *
 * The profile is validated locally, saved locally immediately,
 * and then synced to Firestore in the background.
 */
public class AccountCreateFragment extends Fragment {

    private FragmentAccountCreateBinding binding;

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
        final Button createProfileButton = binding.createAccount;
        final Button cancelAccountButton = binding.cancelAccountCreation;

        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed while text changes
            }

            @Override
            public void afterTextChanged(Editable s) {
                clearFieldErrors();
                createProfileButton.setEnabled(isFormValid(false));
            }
        };

        nameEditText.addTextChangedListener(validationWatcher);
        emailEditText.addTextChangedListener(validationWatcher);
        phoneEditText.addTextChangedListener(validationWatcher);

        createProfileButton.setEnabled(isFormValid(false));

        createProfileButton.setOnClickListener(v -> {
            if (!isFormValid(true)) {
                return;
            }

            Profile profile = new Profile(
                    nameEditText.getText().toString().trim(),
                    emailEditText.getText().toString().trim(),
                    phoneEditText.getText().toString().trim()
            );

            ProfileRepository profileRepository = new ProfileRepository(requireContext());

            // Save locally first so the app can proceed immediately
            profileRepository.saveProfileLocally(profile);

            Toast.makeText(requireContext(),
                    R.string.profile_created_message,
                    Toast.LENGTH_SHORT).show();

            // Start Firestore sync in the background
            profileRepository.saveProfileToFirestore(profile, new ProfileRepository.ProfileRepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Firestore save succeeded; no extra UI action needed here
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

            // Navigate immediately after local save
            Navigation.findNavController(v)
                    .navigate(R.id.action_accountCreateFragment_to_mainMenuFragment);
        });

        cancelAccountButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_accountCreateFragment_to_loginFragment));
    }

    /**
     * Validates the profile form fields.
     *
     * @param showErrors true if field-level errors should be shown
     * @return true if the form is valid, false otherwise
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
     * Clears validation errors from all fields.
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