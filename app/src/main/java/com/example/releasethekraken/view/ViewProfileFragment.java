package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.SessionManager;
import com.example.releasethekraken.databinding.FragmentViewProfileBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;
import com.example.releasethekraken.util.AccessibilitySettingsHelper;
import com.google.firebase.auth.FirebaseAuth;

public class ViewProfileFragment extends Fragment {

    private FragmentViewProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentViewProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }

        ProfileRepository profileRepository = new ProfileRepository(requireContext());
        Profile profile = profileRepository.getProfile();

        binding.profileName.setText(getDisplayValue(profile.getName(), getString(R.string.profile_not_set)));
        binding.profileEmail.setText(getDisplayValue(profile.getEmail(), getString(R.string.profile_not_set)));
        binding.profilePhone.setText(getDisplayValue(profile.getPhone(), getString(R.string.profile_phone_not_provided)));

        String imageUrl = profile.getProfileImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(binding.profilePicture);
        }

        binding.homeToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_global_mainMenuFragment)
        );

        binding.notificationsToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_global_notificationFragment)
        );

        binding.profileEditButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("isEditMode", true);

            Navigation.findNavController(v)
                    .navigate(R.id.action_viewProfileFragment_to_accountCreateFragment, bundle);
        });

        binding.profileSignoutButton.setOnClickListener(v -> {
            ProfileRepository repo = new ProfileRepository(requireContext());
            repo.deleteLocalProfile();

            new SessionManager(requireContext()).clearSession();
            FirebaseAuth.getInstance().signOut();

            Navigation.findNavController(v)
                    .navigate(R.id.action_viewProfileFragment_to_loginFragment);
        });

        binding.profileAccountDeleteButton.setOnClickListener(v ->
                showDeleteConfirmationDialog(profile, profileRepository, v)
        );

        SwitchCompat accessibilitySwitch = binding.switchAccessibility;
        SwitchCompat colorBlindSwitch = binding.switchColorblind;

        accessibilitySwitch.setChecked(
                AccessibilitySettingsHelper.isAccessibilityMode(requireContext())
        );

        colorBlindSwitch.setChecked(
                AccessibilitySettingsHelper.isColorBlindMode(requireContext())
        );

        accessibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AccessibilitySettingsHelper.setAccessibilityMode(requireContext(), isChecked);
            AccessibilitySettingsHelper.applyAccessibility(requireView(), requireContext());

            Toast.makeText(
                    requireContext(),
                    isChecked ? "Accessibility Mode Enabled" : "Accessibility Mode Disabled",
                    Toast.LENGTH_SHORT
            ).show();
        });

        colorBlindSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AccessibilitySettingsHelper.setColorBlindMode(requireContext(), isChecked);

            Toast.makeText(
                    requireContext(),
                    isChecked ? "Alternative Color Palette Enabled" : "Alternative Color Palette Disabled",
                    Toast.LENGTH_SHORT
            ).show();

            requireActivity().recreate();
        });

        AccessibilitySettingsHelper.applyAccessibility(view, requireContext());
    }

    private void showDeleteConfirmationDialog(Profile profile, ProfileRepository profileRepository, View view) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    String uid = profile.getUid();

                    profileRepository.deleteLocalProfile();
                    new SessionManager(requireContext()).clearSession();

                    Toast.makeText(requireContext(),
                            R.string.profile_deleted_message,
                            Toast.LENGTH_SHORT).show();

                    profileRepository.deleteProfileFromFirestore(uid,
                            new ProfileRepository.ProfileRepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    FirebaseAuth.getInstance().signOut();
                                }

                                @Override
                                public void onFailure(Exception exception) {
                                    if (!isAdded()) return;
                                    FirebaseAuth.getInstance().signOut();
                                    Toast.makeText(requireContext(),
                                            "Profile deleted locally, but Firestore delete failed: " + exception.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });

                    Navigation.findNavController(view)
                            .navigate(R.id.action_viewProfileFragment_to_loginFragment);
                })
                .setNegativeButton(R.string.cancel_button_text, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getDisplayValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}