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

/**
 * This is the fragment that is responsible for displaying the main menu of the program and offering the
 * option to browse events, browse your events, scan QR codes and will be the platform from which admins
 * can go to view all images and browse all users, but this has not been implemented yet.
 */
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

        // Navigate to Ticket Test (role access, filtering, history)
        binding.ticketTestButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_mainMenuFragment_to_ticketTestFragment)
        );

        // Navigate to Notifications
        binding.notificationsToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_mainMenuFragment_to_notificationFragment)
        );

        // Navigate to QR Scan
        binding.qrScanButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_mainMenuFragment_to_qrScanFragment)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}