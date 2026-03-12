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

import com.example.releasethekraken.MainActivity;
import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.databinding.FragmentCreateEventBinding;
import com.example.releasethekraken.databinding.FragmentViewProfileBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;

public class CreateEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;
    private boolean editEvent, cameFromYourEvents;
    private String eventID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            editEvent = getArguments().getBoolean("editEvent");
            cameFromYourEvents = getArguments().getBoolean("cameFromYourEvents");
            eventID = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
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

        if (editEvent == true) {
            binding.eventCreateWelcome.setText(R.string.edit_event_welcome);
            binding.createEvent.setText(R.string.edit_event_confirm_button);
            //TODO: FILL IN REMAINING FIELDS WITH EXISTING EVENTS INFORMATION
        }

        // Navigate back to main menu
        binding.cancelEventCreation.setOnClickListener(v -> {
            if (editEvent) {
                Bundle args = new Bundle();
                args.putSerializable("UserType", UserRole.ORGANIZER);
                args.putString("eventId", eventID);
                args.putBoolean("cameFromYourEvents", cameFromYourEvents);

                Navigation.findNavController(v)
                        .navigate(R.id.action_createEventFragment_to_eventDetailsFragment, args);
            } else {
                Bundle args = new Bundle();
                args.putBoolean("yourEvents", true);

                Navigation.findNavController(v)
                        .navigate(R.id.action_createEventFragment_to_browseEventsFragment, args);
            }
        });
    }


}