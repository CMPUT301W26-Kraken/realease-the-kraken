package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;
import com.example.releasethekraken.util.QRCodeGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
    private Button viewQrButton;

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

        titleTextView = view.findViewById(R.id.event_title_text);
        descriptionTextView = view.findViewById(R.id.event_description_display);
        registrationStartTextView = view.findViewById(R.id.registration_start_display);
        registrationEndTextView = view.findViewById(R.id.registration_end_display);

        Button signupOptOutButton = view.findViewById(R.id.signup_optout_button);
        Button returnToBrowseButton = view.findViewById(R.id.return_button);
        Button deleteEventButton = view.findViewById(R.id.delete_event_button);
        Button createNotificationButton = view.findViewById(R.id.create_notification_button);
        Button editEventButton = view.findViewById(R.id.edit_event_button);
        Button viewEntrantMapButton = view.findViewById(R.id.view_entrant_map_button);
        viewQrButton = view.findViewById(R.id.view_qr_button);
        Button viewWaitingListButton = view.findViewById(R.id.view_waiting_list_button);
        Button viewCommentsButton = view.findViewById(R.id.view_comments_button);
        Button exportToCsvButton = view.findViewById(R.id.export_csv_button);
        Button redrawButton = view.findViewById(R.id.redraw_button);

        // Toggle visibilities of buttons based on the user's role
        if (userType == UserRole.ENTRANT) {
            viewEntrantMapButton.setVisibility(View.GONE);
            createNotificationButton.setVisibility(View.GONE);
            viewQrButton.setVisibility(View.GONE);
            editEventButton.setVisibility(View.GONE);
            deleteEventButton.setVisibility(View.GONE);
            exportToCsvButton.setVisibility(View.GONE);
            redrawButton.setVisibility(View.GONE);
        } else if (userType == UserRole.ORGANIZER) {
            signupOptOutButton.setVisibility(View.GONE);
        }

        loadEventDetails();

        viewQrButton.setOnClickListener(v -> showQrCodeDialog());
        signupOptOutButton.setOnClickListener(v -> handleSignupToggle(signupOptOutButton));

        returnToBrowseButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("yourEvents", cameFromYourEvents);
            Navigation.findNavController(view).navigate(R.id.action_eventDetailsFragment_to_browseEventsFragment, args);
        });

        deleteEventButton.setOnClickListener(this::showDeleteConfirmationDialog);
        createNotificationButton.setOnClickListener(v -> showCreateNotificationDialog());
        editEventButton.setOnClickListener(this::navigateToEditEvent);

        viewWaitingListButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("adminView", false);
            args.putString(ARG_EVENT_ID, eventId);
            args.putSerializable("userRole", userType);

            Navigation.findNavController(view).navigate(R.id.action_eventDetailsFragment_to_userListFragment, args);
        });

        viewCommentsButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(ARG_EVENT_ID, eventId);
            args.putSerializable("userRole", userType);

            Navigation.findNavController(view).navigate(R.id.action_eventDetailsFragment_to_commentsFragment, args);
        });
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

    //load real event details from Firestore
    private void loadEventDetails() {
        if (eventId == null) return;
        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                String currentUserId = currentUser != null ? currentUser.getUid() : new ProfileRepository(requireContext()).getProfile().getUid();

                // Private events can only be opened by the organizer or an invited entrant.
                if (event.isPrivate()) {
                    if (TextUtils.isEmpty(currentUserId)) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    "You must be logged in to access this event",
                                    Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                        return;
                    }

                    boolean isOrganizer = event.getOrganizerId().equals(currentUserId);
                    boolean isInvited = event.getInvitedUserIds().contains(currentUserId);

                    if (!isOrganizer && !isInvited) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    "You are not invited to this private event",
                                    Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                        return;
                    }
                }

                currentEvent = event;

                // Private events do not have a QR code for joining
                if (currentEvent.isPrivate()) {
                    viewQrButton.setVisibility(View.GONE);
                }

                bindEventToViews();
                checkIfJoined(currentUserId);
            }
            @Override
            public void onError(Exception e) {
                if (isAdded()) Toast.makeText(requireContext(), "Error loading event", Toast.LENGTH_SHORT).show();
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

    private void handleSignupToggle(Button button) {

        //Ethan adding real entrant to sign up
        ProfileRepository profileRepository = new ProfileRepository(requireContext());
        Profile profile = profileRepository.getProfile();
        String entrantId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : profile.getUid(); // fallback to locally cached UID

        //End of Ethan's edit
        if (!isJoined) {
            waitingListService.joinWaitingList(currentEvent, entrantId, new WaitingListService.JoinCallback() {
                @Override
                public void onResult(WaitingListService.JoinResult result) {
                    if (result == WaitingListService.JoinResult.SUCCESS) {
                        isJoined = true;
                        button.setText(R.string.opt_out_button);
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
                        button.setText(R.string.signup_button);
                    }
                }
                @Override public void onError(Exception e) {}
            });
        }
    }

    private void checkIfJoined(String currentUserId) {
        if (currentUserId == null || currentUserId.isEmpty()) return;
        new WaitingListRepository().isUserInWaitingList(eventId, currentUserId, new WaitingListRepository.CheckCallback() {
            @Override
            public void onResult(boolean exists) {
                isJoined = exists;
                if (getView() != null) {
                    Button btn = getView().findViewById(R.id.signup_optout_button);
                    btn.setText(isJoined ? R.string.opt_out_button : R.string.signup_button);
                }
            }
            @Override public void onError(Exception e) {}
        });
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

    private void navigateToEditEvent(View view) {
        Bundle args = new Bundle();
        args.putBoolean("editEvent", true);
        args.putBoolean("cameFromYourEvents", cameFromYourEvents);
        args.putString("eventId", eventId);
        Navigation.findNavController(view).navigate(R.id.action_eventDetailsFragment_to_createEventFragment, args);
    }
}
