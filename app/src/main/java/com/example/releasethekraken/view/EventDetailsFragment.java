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
import androidx.fragment.app.Fragment;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
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
    private Button joinWaitingListButton;

    private WaitingListService waitingListService;
    private EventRepository eventRepository;
    private Event currentEvent;

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
        }

        WaitingListRepository waitingListRepository = new WaitingListRepository();
        waitingListService = new WaitingListService(waitingListRepository);
        eventRepository = new EventRepository();
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

        titleTextView = view.findViewById(R.id.text_event_title);
        descriptionTextView = view.findViewById(R.id.text_event_description);
        registrationStartTextView = view.findViewById(R.id.text_registration_start);
        registrationEndTextView = view.findViewById(R.id.text_registration_end);
        joinWaitingListButton = view.findViewById(R.id.button_join_waiting_list);

        loadEventDetails();

        joinWaitingListButton.setOnClickListener(v -> {
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
    }

    //load real event details from Firestore
    private void loadEventDetails() {
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                bindEventToViews();
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            "Failed to load event: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //displays the event details on screen
    private void bindEventToViews() {
        titleTextView.setText(currentEvent.getTitle());
        descriptionTextView.setText(currentEvent.getDescription());
        registrationStartTextView.setText("Registration opens: "
                + formatMillis(currentEvent.getRegistrationStartMillis()));
        registrationEndTextView.setText("Registration closes: "
                + formatMillis(currentEvent.getRegistrationEndMillis()));
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