package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;

/**
 * A fragment that shows the details of an event that can be accessed when an event is clicked from
 * the your events or browse events pages. It displays all of the relevant information for an event
 * as well as displays control buttons at the bottom that are dependent upon the user's role.
 *
 * When this event is navigated to it takes three arguments
 * String eventId that is the id of the event that is being viewed and is used to fill in fields with
 *  the relevant event information.
 * UserRole userType that informs the fragment of what type of user is accessing the fragment so it can
 *  display the proper control buttons.
 * Boolean cameFromYourEvents that determines if the details page was accessed through the your events
 *  page or the browse all events page and is used for backwards navigability.
 */
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
            userType = (UserRole) getArguments().getSerializable("UserType");
            cameFromYourEvents = getArguments().getBoolean("cameFromYourEvents");
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

        // Toggle visibilities of button groups based on the user's role
        if (userType == UserRole.ENTRANT) {
            organizerButtonGroup.setVisibility(View.GONE);
            entrantButtonGroup.setVisibility(View.VISIBLE);
        } else if (userType == UserRole.ORGANIZER) {
            organizerButtonGroup.setVisibility(View.VISIBLE);
            entrantButtonGroup.setVisibility(View.GONE);
        }

        loadEventDetails();

        signupOptOutButton.setOnClickListener(v -> {
            if (currentEvent == null) {
                Toast.makeText(requireContext(), "Event could not be loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: replace this later with the real entrant/device/profile ID
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

        // Button for deleting events
        deleteEventButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog(v);
        });

        // Button for creating notifications
        createNotificationButton.setOnClickListener(v -> {
            showCreateNotificationDialog();
        });

        // Button for editing events
        editEventButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("editEvent", true);
            args.putBoolean("cameFromYourEvents", cameFromYourEvents); // Needed for backwards traceability
            // TODO: MAYBE USE THE ENUMERATED TYPE HERE, BUT I'M TOO TIRED TO LOOK INTO THAT
            args.putString("eventId", eventId);

            Navigation.findNavController(view)
                    .navigate(R.id.action_eventDetailsFragment_to_createEventFragment, args);
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

    /**
     * This is a method that is used to set all of the display fields to contain the information stored
     * by the event object being accessed
     */
    private void bindEventToViews() {
        titleTextView.setText(currentEvent.getTitle());
        descriptionTextView.setText(currentEvent.getDescription());
        registrationStartTextView.setText(formatMillis(currentEvent.getRegistrationStartMillis()));
        registrationEndTextView.setText(formatMillis(currentEvent.getRegistrationEndMillis()));
    }

    /**
     * This method is used to create a toast dependent on the success of the join event and display it to the user.
     *
     * @param result the JoinResult object that is created by the WaitingListService that says if the request succeeded.
     */
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

    /**
     * A method that is used to fetch the current user ID of the entrant trying to enlist so that they can
     * be properly added to the event's waiting list
     *
     * /@return
     */
    private String getEntrantId() {
        return "testEntrant001";
    }

    /**
     * Converts a time given in milliseconds into a time displayed in YYYY-MM-DD so that it can be displayed.
     *
     * @param millis the time that is stored in milliseconds
     * @return a String version of the date converted into YYYY-MM-DD format
     */
    private String formatMillis(long millis) {
        return DateFormat.format("yyyy-MM-dd HH:mm", millis).toString();
    }

    /**
     * Shows a confirmation dialog before permanently deleting the event.
     *
     * //@param event current event being deleted
     * //@param eventRepository repository used for local and Firestore deletion
     * @param view current fragment view used for navigation
     */
    // TODO: ADD ARGUMENTS (Event event, EventRepository eventRepository)
    private void showDeleteConfirmationDialog(View view) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    // TODO: ADD FIREBASE DELETION OF THE EVENT

                    Toast.makeText(requireContext(),
                            R.string.event_deleted_message,
                            Toast.LENGTH_SHORT).show();

                    Bundle args = new Bundle();
                    args.putBoolean("yourEvents", cameFromYourEvents);

                    Navigation.findNavController(view)
                            .navigate(R.id.action_eventDetailsFragment_to_browseEventsFragment, args);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    /**
     * Creates a text box where an organizer can create a notification and allow them to post it.
     *
     * //@param notificationRespository repository where the notification will be sent to after it is created
     */
    // TODO: ADD ARGUMENTS (NotificationRepository notificationRespository) (if necessary, I'm not entirely sure how creating notifications works)
    /*
    The below code was created by ChatGPT after showing it the function above and asking if there was a way to add an edit text field for notifications instead
     */
    private void showCreateNotificationDialog() {

        EditText input = new EditText(requireContext());
        input.setHint("Enter notification message");
        input.setMinLines(7);
        input.setGravity(Gravity.TOP);

        new AlertDialog.Builder(requireContext())
                .setTitle("Create Notification")
                .setMessage("Enter the notification text:")
                .setView(input)
                .setPositiveButton("Post Notification", (dialog, which) -> {

                    String notificationText = input.getText().toString().trim();

                    if (!notificationText.isEmpty()) {

                        // TODO: Send notification to Firebase

                        Toast.makeText(requireContext(),
                                "Notification created: " + notificationText,
                                Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(requireContext(),
                                "Notification cannot be empty",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}