package com.example.releasethekraken.view.ui.login;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentAccountCreateBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Fragment where users provide their details (name, email, password, etc.) 
 * to create a new account or edit an existing profile.
 */
public class AccountCreateFragment extends Fragment {

    private static final String TAG = "AccountCreateFragment";
    private FragmentAccountCreateBinding binding;
    private FirebaseAuth mAuth;
    private boolean isEditMode = false;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(binding.imageButton);
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
        mAuth = FirebaseAuth.getInstance();
        ProfileRepository profileRepository = new ProfileRepository(requireContext());

        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean("isEditMode", false);
        }

        setupUI(isEditMode, profileRepository);

        binding.imageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.createAccount.setOnClickListener(v -> handleAccountAction(profileRepository));

        binding.cancelAccountCreation.setOnClickListener(v -> {
            if (isEditMode) {
                Navigation.findNavController(v).popBackStack();
            } else {
                Navigation.findNavController(v).navigate(R.id.action_accountCreateFragment_to_loginFragment);
            }
        });
    }

    private void setupUI(boolean isEditMode, ProfileRepository repo) {
        if (isEditMode) {
            binding.accountCreationWelcome.setText(R.string.action_update_profile);
            binding.createAccount.setText(R.string.action_update_profile);
            
            // In edit mode, hide the password field container and divider
            binding.passwordLayout.setVisibility(View.GONE);
            binding.passwordDivider.setVisibility(View.GONE);

            Profile p = repo.getProfile();
            binding.nameCreate.setText(p.getName());
            binding.emailCreate.setText(p.getEmail());
            binding.phoneCreate.setText(p.getPhone());
            if (p.getProfileImageUrl() != null && !p.getProfileImageUrl().isEmpty()) {
                Glide.with(this).load(p.getProfileImageUrl()).circleCrop().into(binding.imageButton);
            }
        } else {
            binding.accountCreationWelcome.setText(R.string.action_create_welcome);
            binding.createAccount.setText("Register Account");
            
            // Show password field container and divider
            binding.passwordLayout.setVisibility(View.VISIBLE);
            binding.passwordDivider.setVisibility(View.VISIBLE);
        }
    }

    private void handleAccountAction(ProfileRepository repo) {
        String name = binding.nameCreate.getText().toString().trim();
        String email = binding.emailCreate.getText().toString().trim();
        String phone = binding.phoneCreate.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.nameCreate.setError("Name is required");
            return;
        }

        if (isEditMode) {
            updateExistingProfile(name, email, phone, repo);
        } else {
            String password = binding.passwordCreate.getText().toString().trim();
            if (password.length() < 6) {
                binding.passwordCreate.setError("Password must be at least 6 characters");
                return;
            }
            createNewAccount(name, email, password, phone, repo);
        }
    }

    private void createNewAccount(String name, String email, String password, String phone, ProfileRepository repo) {
        binding.loading.setVisibility(View.VISIBLE);
        binding.createAccount.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Profile newProfile = new Profile(name, email, phone);
                        newProfile.setUid(user.getUid());

                        if (selectedImageUri != null) {
                            repo.uploadProfileImage(selectedImageUri, user.getUid(), new ProfileRepository.ProfileRepositoryCallback<String>() {
                                @Override
                                public void onSuccess(String url) {
                                    newProfile.setProfileImageUrl(url);
                                    saveProfileAndFinish(newProfile, repo);
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    saveProfileAndFinish(newProfile, repo);
                                }
                            });
                        } else {
                            saveProfileAndFinish(newProfile, repo);
                        }
                    } else {
                        binding.loading.setVisibility(View.GONE);
                        binding.createAccount.setEnabled(true);
                        Toast.makeText(getContext(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateExistingProfile(String name, String email, String phone, ProfileRepository repo) {
        Profile p = repo.getProfile();
        p.setName(name);
        p.setEmail(email);
        p.setPhone(phone);
        
        if (selectedImageUri != null) {
            binding.loading.setVisibility(View.VISIBLE);
            binding.createAccount.setEnabled(false);
            repo.uploadProfileImage(selectedImageUri, p.getUid(), new ProfileRepository.ProfileRepositoryCallback<String>() {
                @Override
                public void onSuccess(String url) {
                    p.setProfileImageUrl(url);
                    saveProfileAndFinish(p, repo);
                }
                @Override
                public void onFailure(Exception e) {
                    saveProfileAndFinish(p, repo);
                }
            });
        } else {
            saveProfileAndFinish(p, repo);
        }
    }

    private void saveProfileAndFinish(Profile profile, ProfileRepository repo) {
        repo.saveProfileToFirestore(profile, new ProfileRepository.ProfileRepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (!isAdded()) return;
                repo.saveProfileLocally(profile);
                Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigate(R.id.action_global_mainMenuFragment);
            }
            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                binding.loading.setVisibility(View.GONE);
                binding.createAccount.setEnabled(true);
                Toast.makeText(getContext(), "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}