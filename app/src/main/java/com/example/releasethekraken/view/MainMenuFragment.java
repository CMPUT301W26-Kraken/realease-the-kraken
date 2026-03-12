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
import com.example.releasethekraken.databinding.FragmentMainMenuBinding;

public class MainMenuFragment extends Fragment {

    private FragmentMainMenuBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentMainMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
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

        // Navigate to View Profile
        binding.profileToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_mainMenuFragment_to_viewProfileFragment)
        );

        // Navigate to Browse Events
        binding.browseEventsButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("yourEvents", false); // Using this argument to determine what should be displayed

            Navigation.findNavController(v)
                    .navigate(R.id.action_mainMenuFragment_to_browseEventsFragment, bundle);
        });

        // Navigate to Your Events
        binding.yourEventsButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("yourEvents", true); // Using this argument to determine what should be displayed

            Navigation.findNavController(v)
                    .navigate(R.id.action_mainMenuFragment_to_browseEventsFragment, bundle);
        });

        // Navigate to Notifications
        binding.notificationsToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_mainMenuFragment_to_notificationFragment)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}