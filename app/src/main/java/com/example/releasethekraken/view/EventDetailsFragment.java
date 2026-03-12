package com.example.releasethekraken.view;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.MainActivity;
import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.WaitingListRepository;

//fragment that shows the details for one event and allows the entrant
//to join the waiting list
public class EventDetailsFragment extends Fragment {

    public static final String ARG_EVENT_ID = "eventId";

    private String eventId;

    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView registrationStartTextView;
    private TextView registrationEndTextView;
    private Button signupOptOutButton;
    private Button returnToBrowseButton;

    private WaitingListService waitingListService;
    private Event currentEvent;
    private MainActivity.UserType userType;
    private boolean cameFromYourEvents;

    public EventDetailsFragment() {
        // required empty public constructor
    }

    public static EventDetailsFragment newInstance(String eventId) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            userType = (MainActivity.UserType) getArguments().getSerializable("UserType");
            cameFromYourEvents = getArguments().getBoolean("cameFromYourEvents");
        }

        WaitingListRepository waitingListRepository = new WaitingListRepository();
        waitingListService = new WaitingListService(waitingListRepository);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Group organizerButtonGroup = view.findViewById(R.id.organizer_button_group);

        titleTextView = view.findViewById(R.id.event_title_text);
        descriptionTextView = view.findViewById(R.id.event_description_display);
        registrationStartTextView = view.findViewById(R.id.registration_start_display);
        registrationEndTextView = view.findViewById(R.id.registration_end_display);
        signupOptOutButton = view.findViewById(R.id.signup_optout_button);
        returnToBrowseButton = view.findViewById(R.id.return_button);
        
        if (userType == MainActivity.UserType.ENTRANT) {
            organizerButtonGroup.setVisibility(View.GONE);
        } else if (userType == MainActivity.UserType.ORGANIZER) {
            signupOptOutButton.setVisibility(View.GONE);
        }

        loadEventDetails();

        signupOptOutButton.setOnClickListener(v -> {
            if (currentEvent == null) {
                Toast.makeText(requireContext(), "Event could not be loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            // replace this later with the real entrant/device/profile ID
            String entrantId = getEntrantId();

            waitingListService.joinWaitingList(currentEvent, entrantId, new WaitingListService.JoinCallback() {
                @Override
                public void onResult(WaitingListService.JoinResult result) {
                    handleJoinResult(result);
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(requireContext(),
                            "Error joining waiting list: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Button that returns to the browse events
        returnToBrowseButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("yourEvents", cameFromYourEvents);

            Navigation.findNavController(view)
                    .navigate(R.id.action_eventDetailsFragment_to_browseEventsFragment, args);
        });
    }

    //Temporary event loader
    //Later, replace this with EventRepository.getEventById(eventId, ...)
    private void loadEventDetails() {
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();

        // Temporary hardcoded event so the screen works end-to-end
        currentEvent = new Event(
                eventId,
                now - 60_000,
                now + 3_600_000
        );

        bindEventToViews();
    }

    //displays the event details on screen

    private void bindEventToViews() {
        titleTextView.setText("Event " + currentEvent.getEventId());
        descriptionTextView.setText("This is a placeholder event description.");
        registrationStartTextView.setText(formatMillis(currentEvent.getRegistrationStartMillis()));
        registrationEndTextView.setText(formatMillis(currentEvent.getRegistrationEndMillis()));
    }

    //Handles the result of a join waiting list request
    private void handleJoinResult(WaitingListService.JoinResult result) {
        if (!isAdded()) {
            return;
        }

        switch (result) {
            case SUCCESS:
                Toast.makeText(requireContext(),
                        "Successfully joined the waiting list",
                        Toast.LENGTH_SHORT).show();
                break;

            case DUPLICATE_ENTRY:
                Toast.makeText(requireContext(),
                        "You are already on the waiting list",
                        Toast.LENGTH_SHORT).show();
                break;

            case REGISTRATION_CLOSED:
                Toast.makeText(requireContext(),
                        "Registration is closed",
                        Toast.LENGTH_SHORT).show();
                break;

            case INVALID_INPUT:
                Toast.makeText(requireContext(),
                        "Invalid event or entrant information",
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    //Temporary entrant ID for testing.
    //replace later with the real profile/device/user ID

    private String getEntrantId() {
        return "testEntrant001";
    }

    //formats milliseconds into a readable time
    private String formatMillis(long millis) {
        return DateFormat.format("yyyy-MM-dd HH:mm", millis).toString();
    }
}