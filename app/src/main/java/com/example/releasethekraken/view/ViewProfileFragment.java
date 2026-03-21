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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentViewProfileBinding;
import com.example.releasethekraken.repository.ProfileRepository;
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
        com.example.releasethekraken.model.Profile profile = profileRepository.getProfile();

        binding.profileName.setText(getDisplayValue(profile.getName(), getString(R.string.profile_not_set)));
        binding.profileEmail.setText(getDisplayValue(profile.getEmail(), getString(R.string.profile_not_set)));
        binding.profilePhone.setText(getDisplayValue(profile.getPhone(), getString(R.string.profile_phone_not_provided)));

        binding.homeToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_viewProfileFragment_to_mainMenuFragment)
        );

        binding.notificationsToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_viewProfileFragment_to_notificationFragment)
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

            FirebaseAuth.getInstance().signOut();

            Navigation.findNavController(v)
                    .navigate(R.id.action_viewProfileFragment_to_loginFragment);
        });

        binding.profileAccountDeleteButton.setOnClickListener(v ->
                showDeleteConfirmationDialog(profileRepository, v)
        );
    }

    private void showDeleteConfirmationDialog(ProfileRepository profileRepository, View view) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_profile_title)
                .setMessage(R.string.delete_profile_confirmation)
                .setPositiveButton(R.string.delete_button_text, (dialog, which) -> {
                    profileRepository.deleteLocalProfile();

                    profileRepository.deleteProfileFromFirestore(new ProfileRepository.ProfileRepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (!isAdded()) {
                                return;
                            }

                            Toast.makeText(requireContext(),
                                    R.string.profile_deleted_message,
                                    Toast.LENGTH_SHORT).show();

                            FirebaseAuth.getInstance().signOut();

                            Navigation.findNavController(view)
                                    .navigate(R.id.action_viewProfileFragment_to_loginFragment);
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            if (!isAdded()) {
                                return;
                            }

                            Toast.makeText(requireContext(),
                                    "Profile deleted locally, but Firestore delete failed: " + exception.getMessage(),
                                    Toast.LENGTH_LONG).show();

                            FirebaseAuth.getInstance().signOut();

                            Navigation.findNavController(view)
                                    .navigate(R.id.action_viewProfileFragment_to_loginFragment);
                        }
                    });
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