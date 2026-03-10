package com.example.releasethekraken.view;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentNotificationBinding;
import com.example.releasethekraken.databinding.FragmentViewProfileBinding;

public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ensure ActionBar is visible
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }

        // Navigate back to main menu
        binding.homeToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_notificationFragment_to_mainMenuFragment)
        );

        // Navigate to the Profile
        binding.profileToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_notificationFragment_to_viewProfileFragment)
        );

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}