package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.util.QRCodeGenerator;

public class EventDetailsFragment extends Fragment {

    public static final String ARG_EVENT_ID = "eventId";

    private String eventId;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView registrationStartTextView;
    private TextView registrationEndTextView;

    private WaitingListService waitingListService;
    private EventRepository eventRepository;
    private Event currentEvent;
    private UserRole userType;
    private boolean cameFromYourEvents;

    private boolean isJoined = false;

    public EventDetailsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            if (eventId == null) {
                eventId = getArguments().getString("eventId");
            }
            userType = (UserRole) getArguments().getSerializable("UserType");
            cameFromYourEvents = getArguments().getBoolean("cameFromYourEvents");
        }

        waitingListService = new WaitingListService(new WaitingListRepository());
        eventRepository = new EventRepository();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Group organizerButtonGroup = view.findViewById(R.id.organizer_button_group);
        Group entrantButtonGroup = view.findViewById(R.id.entrant_button_group);

        titleTextView = view.findViewById(R.id.event_title_text);
        descriptionTextView = view.findViewById(R.id.event_description_display);
        registrationStartTextView = view.findViewById(R.id.registration_start_display);
        registrationEndTextView = view.findViewById(R.id.registration_end_display);
        
        Button signupOptOutButton = view.findViewById(R.id.signup_optout_button);
        Button returnToBrowseButton = view.findViewById(R.id.return_button);
        Button deleteEventButton = view.findViewById(R.id.delete_event_button);
        Button createNotificationButton = view.findViewById(R.id.create_notification_button);
        Button editEventButton = view.findViewById(R.id.edit_event_button);
        Button viewQrButton = view.findViewById(R.id.view_qr_button);

        if (userType == UserRole.ENTRANT) {
            organizerButtonGroup.setVisibility(View.GONE);
            entrantButtonGroup.setVisibility(View.VISIBLE);
        } else {
            organizerButtonGroup.setVisibility(View.VISIBLE);
            entrantButtonGroup.setVisibility(View.GONE);
        }

        loadEventDetails();

        viewQrButton.setOnClickListener(v -> showQrCodeDialog());
        signupOptOutButton.setOnClickListener(v -> handleSignupToggle(signupOptOutButton));

        returnToBrowseButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("yourEvents", cameFromYourEvents);
            Navigation.findNavController(view).navigate(R.id.action_eventDetailsFragment_to_browseEventsFragment, args);
        });

        deleteEventButton.setOnClickListener(v -> showDeleteConfirmationDialog(v));
        createNotificationButton.setOnClickListener(v -> showCreateNotificationDialog());
        editEventButton.setOnClickListener(v -> navigateToEditEvent(view));
    }

    private void showQrCodeDialog() {
        if (eventId == null) return;

        // Content matches what the scanner expects: "event:ID"
        String qrContent = "event:" + eventId;
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrContent, 500, 500);

        if (qrBitmap != null) {
            ImageView imageView = new ImageView(requireContext());
            imageView.setImageBitmap(qrBitmap);
            imageView.setPadding(20, 20, 20, 20);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Event QR Code")
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .show();
        } else {
            Toast.makeText(requireContext(), "Failed to generate QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadEventDetails() {
        if (eventId == null) return;
        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                bindEventToViews();
                checkIfJoined();
            }
            @Override
            public void onError(Exception e) {
                if (isAdded()) Toast.makeText(requireContext(), "Error loading event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindEventToViews() {
        titleTextView.setText(currentEvent.getTitle());
        descriptionTextView.setText(currentEvent.getDescription());
        registrationStartTextView.setText(formatMillis(currentEvent.getRegistrationStartMillis()));
        registrationEndTextView.setText(formatMillis(currentEvent.getRegistrationEndMillis()));
    }

    private void handleSignupToggle(Button button) {
        String entrantId = "testEntrant001"; 
        if (!isJoined) {
            waitingListService.joinWaitingList(currentEvent, entrantId, new WaitingListService.JoinCallback() {
                @Override
                public void onResult(WaitingListService.JoinResult result) {
                    if (result == WaitingListService.JoinResult.SUCCESS) {
                        isJoined = true;
                        button.setText("Leave Waiting List");
                    }
                    Toast.makeText(requireContext(), result.name(), Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(Exception e) {}
            });
        } else {
            waitingListService.leaveWaitingList(currentEvent, entrantId, new WaitingListService.LeaveCallback() {
                @Override
                public void onResult(WaitingListService.LeaveResult result) {
                    if (result == WaitingListService.LeaveResult.SUCCESS) {
                        isJoined = false;
                        button.setText("Join Waiting List");
                    }
                }
                @Override public void onError(Exception e) {}
            });
        }
    }

    private void checkIfJoined() {
        new WaitingListRepository().isUserInWaitingList(eventId, "testEntrant001", new WaitingListRepository.CheckCallback() {
            @Override
            public void onResult(boolean exists) {
                isJoined = exists;
                if (getView() != null) {
                    Button btn = getView().findViewById(R.id.signup_optout_button);
                    btn.setText(isJoined ? "Leave Waiting List" : "Join Waiting List");
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    private String formatMillis(long millis) {
        return DateFormat.format("yyyy-MM-dd HH:mm", millis).toString();
    }

    private void showDeleteConfirmationDialog(View v) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Event Deleted", Toast.LENGTH_SHORT).show();
                    Bundle args = new Bundle();
                    args.putBoolean("yourEvents", cameFromYourEvents);
                    Navigation.findNavController(v).navigate(R.id.action_eventDetailsFragment_to_browseEventsFragment, args);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateNotificationDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Notification Message");
        new AlertDialog.Builder(requireContext())
                .setTitle("Create Notification")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> Toast.makeText(requireContext(), "Notification Sent", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToEditEvent(View view) {
        Bundle args = new Bundle();
        args.putBoolean("editEvent", true);
        args.putBoolean("cameFromYourEvents", cameFromYourEvents);
        args.putString("eventId", eventId);
        Navigation.findNavController(view).navigate(R.id.action_eventDetailsFragment_to_createEventFragment, args);
    }
}
