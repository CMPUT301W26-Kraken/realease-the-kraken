package com.example.releasethekraken.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentViewProfileBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.repository.ProfileRepository;

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

        binding.backToMainButton.setOnClickListener(v ->
        // Navigate back to main menu
        binding.homeToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_viewProfileFragment_to_mainMenuFragment)
        );

        // Navigate to the notifications
        binding.notificationsToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_viewProfileFragment_to_notificationFragment)
        );

        // Sign out and go to login screen
        binding.profileSignoutButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_viewProfileFragment_to_loginFragment)
        );
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
