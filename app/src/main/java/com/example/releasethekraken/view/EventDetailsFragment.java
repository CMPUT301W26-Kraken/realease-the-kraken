package com.example.releasethekraken.view;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.releasethekraken.MainActivity;
import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentEventDetailsBinding;

public class EventDetailsFragment extends Fragment {

    private FragmentEventDetailsBinding binding;
    private MainActivity.UserType userType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentEventDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            String userTypeStr = getArguments().getString("UserType");
            if (userTypeStr != null) {
                userType = MainActivity.UserType.valueOf(userTypeStr);
            } else {
                userType = MainActivity.UserType.ENTRANT; // default
            }
        }

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }

        Group organizerButtonGroup = view.findViewById(R.id.organizer_button_group);
        Button signupOptOutButton = view.findViewById((R.id.signup_optout_button));
        // Handle Visibilities of the button groups
        if (userType == MainActivity.UserType.ENTRANT) {
            organizerButtonGroup.setVisibility(View.GONE);
        } else if (userType == MainActivity.UserType.ORGANIZER) {
            signupOptOutButton.setVisibility(View.GONE);
        }

        // Sign out and return to login screen
        binding.returnButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_eventDetailsFragment_to_browseEventsFragment)
        );
    }
}