package com.example.releasethekraken.view;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.NotificationService;
import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.NotificationRepository;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;
import com.example.releasethekraken.util.LocationHelper;
import com.example.releasethekraken.util.QRCodeGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EventDetailsFragment extends Fragment {

    public static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_SEARCH_QUERY = "searchQuery";
    private static final String ARG_FILTER_AVAILABLE_AT = "filterAvailableAt";
    private static final String ARG_FILTER_CAPACITY = "filterCapacity";
    public static final String ARG_IS_PRIVATE = "isPrivate";
    private static final long MAX_POSTER_BYTES = 5L * 1024L * 1024L;

    private String eventId;
    private Boolean isPrivateFromArgs;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView registrationStartTextView;
    private TextView registrationEndTextView;
    private TextView waitingListCountTextView;
    private ImageView posterImageView;
    private Button viewQrButton;

    private Button signupOptOutButton;
    private Button deleteEventButton;
    private Button createNotificationButton;
    private Button editEventButton;
    private Button viewEntrantMapButton;
    private Button exportToCsvButton;
    private Button redrawButton;

    private WaitingListRepository waitingListRepository;
    private WaitingListService waitingListService;
    private EventRepository eventRepository;
    private NotificationService notificationService;
    private Event currentEvent;
    private UserRole userType;
    private boolean cameFromYourEvents;
    private boolean isJoined = false;
    private String browseSearchQuery = "";
    private String browseFilterAvailableAt = "";
    private String browseFilterCapacity = "";

    private ActivityResultLauncher<String> locationPermissionLauncher;
    private Button pendingSignupButton;
    private ListenerRegistration waitingListCountListener;

    public EventDetailsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            if (eventId == null) {
                eventId = getArguments().getString("eventId");
            }
            if (getArguments().containsKey(ARG_IS_PRIVATE)) {
                isPrivateFromArgs = getArguments().getBoolean(ARG_IS_PRIVATE);
            }
            userType = (UserRole) getArguments().getSerializable("UserType");
            cameFromYourEvents = getArguments().getBoolean("cameFromYourEvents");
            browseSearchQuery = getArguments().getString(ARG_SEARCH_QUERY, "");
            browseFilterAvailableAt = getArguments().getString(ARG_FILTER_AVAILABLE_AT, "");
            browseFilterCapacity = getArguments().getString(ARG_FILTER_CAPACITY, "");
        }

        waitingListRepository = new WaitingListRepository();
        waitingListService = new WaitingListService(waitingListRepository);
        eventRepository = new EventRepository();
        notificationService = new NotificationService(new NotificationRepository());

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (pendingSignupButton != null) {
                        if (granted) {
                            joinWithLocation(pendingSignupButton);
                        } else {
                            joinWaitingListWithCoords(pendingSignupButton, 0.0, 0.0);
                        }
                        pendingSignupButton = null;
                    }
                });
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
        waitingListCountTextView = view.findViewById(R.id.waiting_list_count_text);
        posterImageView = view.findViewById(R.id.event_poster);

        signupOptOutButton = view.findViewById(R.id.signup_optout_button);
        Button returnToBrowseButton = view.findViewById(R.id.return_button);
        deleteEventButton = view.findViewById(R.id.delete_event_button);
        createNotificationButton = view.findViewById(R.id.create_notification_button);
        editEventButton = view.findViewById(R.id.edit_event_button);
        viewEntrantMapButton = view.findViewById(R.id.view_entrant_map_button);
        viewQrButton = view.findViewById(R.id.view_qr_button);
        Button viewWaitingListButton = view.findViewById(R.id.view_waiting_list_button);
        Button viewCommentsButton = view.findViewById(R.id.view_comments_button);
        exportToCsvButton = view.findViewById(R.id.export_csv_button);
        redrawButton = view.findViewById(R.id.redraw_button);

        updateUIForRole();

        // Apply initial visibility based on arguments to prevent flicker/delay
        if (isPrivateFromArgs != null) {
            viewQrButton.setVisibility(isPrivateFromArgs ? View.GONE : View.VISIBLE);
        }

        loadEventDetails();
        startWaitingListCountListener();

        viewQrButton.setOnClickListener(v -> showQrCodeDialog());
        signupOptOutButton.setOnClickListener(v -> handleSignupToggle(signupOptOutButton));

        returnToBrowseButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("yourEvents", cameFromYourEvents);
            args.putString(ARG_SEARCH_QUERY, browseSearchQuery);
            args.putString(ARG_FILTER_AVAILABLE_AT, browseFilterAvailableAt);
            args.putString(ARG_FILTER_CAPACITY, browseFilterCapacity);
            Navigation.findNavController(view).navigate(R.id.action_eventDetailsFragment_to_browseEventsFragment, args);
        });

        deleteEventButton.setOnClickListener(this::showDeleteConfirmationDialog);
        createNotificationButton.setOnClickListener(v -> showCreateNotificationDialog());
        editEventButton.setOnClickListener(this::navigateToEditEvent);

        viewEntrantMapButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(ARG_EVENT_ID, eventId);
            Navigation.findNavController(v).navigate(R.id.action_eventDetailsFragment_to_entrantMapFragment, args);
        });

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

    private void updateUIForRole() {
        if (userType == UserRole.ENTRANT) {
            viewEntrantMapButton.setVisibility(View.GONE);
            createNotificationButton.setVisibility(View.GONE);
            editEventButton.setVisibility(View.GONE);
            deleteEventButton.setVisibility(View.GONE);
            exportToCsvButton.setVisibility(View.GONE);
            redrawButton.setVisibility(View.GONE);
            signupOptOutButton.setVisibility(View.VISIBLE);
        } else if (userType == UserRole.ORGANIZER || userType == UserRole.CO_ORGANIZER || userType == UserRole.ADMIN) {
            viewEntrantMapButton.setVisibility(View.VISIBLE);
            createNotificationButton.setVisibility(View.VISIBLE);
            editEventButton.setVisibility(View.VISIBLE);
            deleteEventButton.setVisibility(View.VISIBLE);
            exportToCsvButton.setVisibility(View.VISIBLE);
            redrawButton.setVisibility(View.VISIBLE);
            signupOptOutButton.setVisibility(View.GONE);
        }
    }

    private void startWaitingListCountListener() {
        if (TextUtils.isEmpty(eventId)) {
            return;
        }

        if (waitingListCountListener != null) {
            waitingListCountListener.remove();
        }

        waitingListCountListener = waitingListRepository.listenForWaitingListCount(
                eventId,
                new WaitingListRepository.WaitingListCountCallback() {
                    @Override
                    public void onCountChanged(int count) {
                        if (!isAdded() || waitingListCountTextView == null) {
                            return;
                        }
                        waitingListCountTextView.setText(
                                getString(R.string.waiting_list_count_text, count)
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded() || waitingListCountTextView == null) {
                            return;
                        }
                        waitingListCountTextView.setText(
                                getString(R.string.waiting_list_count_unavailable)
                        );
                    }
                }
        );
    }

    private void showQrCodeDialog() {
        if (eventId == null || currentEvent == null || currentEvent.isPrivate()) {
            return;
        }

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
        if (eventId == null) {
            return;
        }

        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                String currentUserId = getCurrentEntrantId();

                if (TextUtils.isEmpty(currentUserId)) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "You must be logged in to access this event", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                    }
                    return;
                }

                // Determine user role
                if (event.getOrganizerId().equals(currentUserId)) {
                    userType = UserRole.ORGANIZER;
                } else if (event.getCoOrganizerIds().contains(currentUserId)) {
                    userType = UserRole.CO_ORGANIZER;
                } else if (userType != UserRole.ADMIN) {
                    userType = UserRole.ENTRANT;
                }

                if (event.isPrivate()) {
                    boolean isOrganizer = userType == UserRole.ORGANIZER || userType == UserRole.CO_ORGANIZER || userType == UserRole.ADMIN;
                    boolean isInvited = event.getInvitedUserIds().contains(currentUserId);
                    if (!isOrganizer && !isInvited) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "You are not invited to this private event", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                        return;
                    }
                }

                currentEvent = event;
                if (currentEvent.isPrivate()) {
                    viewQrButton.setVisibility(View.GONE);
                } else {
                    viewQrButton.setVisibility(View.VISIBLE);
                }
                
                if (isAdded()) {
                    updateUIForRole();
                    bindEventToViews();
                    checkIfJoined(currentUserId);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading event", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindEventToViews() {
        titleTextView.setText(currentEvent.getTitle());
        descriptionTextView.setText(currentEvent.getDescription());
        registrationStartTextView.setText(formatMillis(currentEvent.getRegistrationStartMillis()));
        registrationEndTextView.setText(formatMillis(currentEvent.getRegistrationEndMillis()));
        loadPosterIntoView(currentEvent.getPosterUrl());
    }

    private void handleSignupToggle(Button button) {
        String entrantId = getCurrentEntrantId();
        if (TextUtils.isEmpty(entrantId)) {
            Toast.makeText(requireContext(), "Create a profile before joining the waiting list", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isJoined) {
            if (currentEvent.isGeolocationRequired()) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    joinWithLocation(button);
                } else {
                    pendingSignupButton = button;
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            } else {
                joinWaitingListWithCoords(button, 0.0, 0.0);
            }
        } else {
            waitingListService.leaveWaitingList(currentEvent, entrantId, new WaitingListService.LeaveCallback() {
                @Override
                public void onResult(WaitingListService.LeaveResult result) {
                    if (result == WaitingListService.LeaveResult.SUCCESS) {
                        isJoined = false;
                        button.setText(R.string.signup_button);
                    }
                }

                @Override
                public void onError(Exception e) {}
            });
        }
    }

    private void joinWithLocation(Button button) {
        LocationHelper.getLastLocation(requireContext(), new LocationHelper.LocationCallback() {
            @Override
            public void onLocation(double latitude, double longitude) {
                joinWaitingListWithCoords(button, latitude, longitude);
            }

            @Override
            public void onError(String reason) {
                joinWaitingListWithCoords(button, 0.0, 0.0);
            }
        });
    }

    private void joinWaitingListWithCoords(Button button, double latitude, double longitude) {
        String entrantId = getCurrentEntrantId();
        waitingListService.joinWaitingList(currentEvent, entrantId, latitude, longitude,
                new WaitingListService.JoinCallback() {
                    @Override
                    public void onResult(WaitingListService.JoinResult result) {
                        if (result == WaitingListService.JoinResult.SUCCESS) {
                            isJoined = true;
                            button.setText(R.string.opt_out_button);
                        } else if (result == WaitingListService.JoinResult.ALREADY_ORGANIZER) {
                            Toast.makeText(requireContext(), "Organizers cannot join the waiting list", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), result.name(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {}
                });
    }

    private void checkIfJoined(String currentUserId) {
        if (TextUtils.isEmpty(currentUserId)) {
            return;
        }

        waitingListRepository.isUserInWaitingList(eventId, currentUserId, new WaitingListRepository.CheckCallback() {
            @Override
            public void onResult(boolean exists) {
                isJoined = exists;
                if (getView() != null && signupOptOutButton != null) {
                    signupOptOutButton.setText(isJoined ? R.string.opt_out_button : R.string.signup_button);
                }
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private String getCurrentEntrantId() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }

        Profile profile = new ProfileRepository(requireContext()).getProfile();
        if (profile != null && !TextUtils.isEmpty(profile.getUid())) {
            return profile.getUid();
        }
        return "";
    }

    private void loadPosterIntoView(String posterUrl) {
        if (TextUtils.isEmpty(posterUrl)) {
            posterImageView.setImageResource(R.drawable.krakenlogov1);
            return;
        }

        FirebaseStorage.getInstance()
                .getReferenceFromUrl(posterUrl)
                .getBytes(MAX_POSTER_BYTES)
                .addOnSuccessListener(bytes -> {
                    if (isAdded()) {
                        posterImageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        posterImageView.setImageResource(R.drawable.krakenlogov1);
                    }
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
        input.setHint(getString(R.string.notify_selected_entrants_hint));
        input.setMinLines(7);
        input.setGravity(Gravity.TOP);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.create_notification_button)
                .setView(input)
                .setPositiveButton(R.string.post_notification_button, (dialog, which) ->
                        notifySelectedEntrants(input.getText().toString().trim()))
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void notifySelectedEntrants(String organizerMessage) {
        if (currentEvent == null || TextUtils.isEmpty(eventId)) {
            Toast.makeText(requireContext(), R.string.notification_send_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        waitingListRepository.getAcceptedEntrants(eventId, new WaitingListRepository.EntrantsCallback() {
            @Override
            public void onResult(List<String> entrants) {
                if (!isAdded()) {
                    return;
                }
                if (entrants.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.notify_selected_entrants_none_selected, Toast.LENGTH_SHORT).show();
                    return;
                }

                AtomicInteger completedCount = new AtomicInteger(0);
                AtomicInteger successCount = new AtomicInteger(0);
                int total = entrants.size();

                for (String entrantId : entrants) {
                    notificationService.sendSelectedEntrantNotification(
                            currentEvent,
                            entrantId,
                            organizerMessage,
                            new NotificationService.NotificationCallback() {
                                @Override
                                public void onResult(NotificationService.NotificationResult result) {
                                    if (result == NotificationService.NotificationResult.SUCCESS) {
                                        successCount.incrementAndGet();
                                    }
                                    maybeFinish();
                                }

                                @Override
                                public void onError(Exception e) {
                                    maybeFinish();
                                }

                                private void maybeFinish() {
                                    if (completedCount.incrementAndGet() == total && isAdded()) {
                                        Toast.makeText(
                                                requireContext(),
                                                getString(R.string.notify_selected_entrants_success, successCount.get(), total),
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                }
                            }
                    );
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.notification_send_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void navigateToEditEvent(View view) {
        Bundle args = new Bundle();
        args.putBoolean("editEvent", true);
        args.putBoolean("cameFromYourEvents", cameFromYourEvents);
        args.putString("eventId", eventId);
        Navigation.findNavController(view).navigate(R.id.action_eventDetailsFragment_to_createEventFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (waitingListCountListener != null) {
            waitingListCountListener.remove();
            waitingListCountListener = null;
        }
    }
}
