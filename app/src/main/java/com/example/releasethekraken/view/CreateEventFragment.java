package com.example.releasethekraken.view;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.DrawEntrantsWorker;
import com.example.releasethekraken.controller.NotificationService;
import com.example.releasethekraken.databinding.FragmentCreateEventBinding;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.NotificationRepository;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.repository.ProfileRepository;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CreateEventFragment extends Fragment {
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy h:mm a";

    private FragmentCreateEventBinding binding;
    private boolean editEvent;
    private boolean cameFromYourEvents;
    private String eventID;
    private Uri selectedPosterUri;
    private String existingPosterUrl = "";
    
    private final ArrayList<String> invitedUserIds = new ArrayList<>();
    private final ArrayList<String> coOrganizerIds = new ArrayList<>();
    
    // For bracket display
    private final ArrayList<String> invitedUserNames = new ArrayList<>();
    private final ArrayList<String> coOrganizerNames = new ArrayList<>();

    private NotificationService notificationService;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || binding == null) {
                    return;
                }
                selectedPosterUri = uri;
                Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .into(binding.imageButton);
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationService = new NotificationService(new NotificationRepository());
        if (getArguments() != null) {
            editEvent = getArguments().getBoolean("editEvent");
            cameFromYourEvents = getArguments().getBoolean("cameFromYourEvents");
            eventID = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }

        if (editEvent) {
            binding.eventCreateWelcome.setText(R.string.edit_event_welcome);
            binding.createEvent.setText(R.string.edit_event_confirm_button);
            loadEventForEditing();
        } else {
            updatePreviews();
        }

        binding.imageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.uploadPosterText.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.privateEventSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.invitedGuestsLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Co-organizer logic
        binding.addCoOrganizerButton.setOnClickListener(v -> {
            String input = binding.coOrganizerSearchInput.getText().toString().trim();
            if (TextUtils.isEmpty(input)) return;

            binding.addCoOrganizerButton.setEnabled(false);
            new ProfileRepository(requireContext()).searchProfiles(input, new ProfileRepository.ProfileRepositoryCallback<Profile>() {
                @Override
                public void onSuccess(Profile profile) {
                    if (!isAdded()) return;
                    binding.addCoOrganizerButton.setEnabled(true);
                    if (profile != null && !TextUtils.isEmpty(profile.getUid())) {
                        String uid = profile.getUid();
                        if (invitedUserIds.contains(uid)) {
                            Toast.makeText(getContext(), "User is already invited as a guest", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!coOrganizerIds.contains(uid)) {
                            coOrganizerIds.add(uid);
                            coOrganizerNames.add(profile.getName());
                            binding.coOrganizerSearchInput.setText("");
                            updatePreviews();
                            Toast.makeText(getContext(), "Added " + profile.getName() + " as co-organizer", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Already a co-organizer", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    if (!isAdded()) return;
                    binding.addCoOrganizerButton.setEnabled(true);
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Guest logic
        binding.addGuestInviteButton.setOnClickListener(v -> {
            String input = binding.guestInviteSearchInput.getText().toString().trim();
            if (TextUtils.isEmpty(input)) return;

            binding.addGuestInviteButton.setEnabled(false);
            new ProfileRepository(requireContext()).searchProfiles(input, new ProfileRepository.ProfileRepositoryCallback<Profile>() {
                @Override
                public void onSuccess(Profile profile) {
                    if (!isAdded()) return;
                    binding.addGuestInviteButton.setEnabled(true);
                    if (profile != null && !TextUtils.isEmpty(profile.getUid())) {
                        String uid = profile.getUid();
                        if (coOrganizerIds.contains(uid)) {
                            Toast.makeText(getContext(), "User is already a co-organizer", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!invitedUserIds.contains(uid)) {
                            invitedUserIds.add(uid);
                            invitedUserNames.add(profile.getName());
                            binding.guestInviteSearchInput.setText("");
                            updatePreviews();
                            Toast.makeText(getContext(), "Invited " + profile.getName(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "User already invited", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    if (!isAdded()) return;
                    binding.addGuestInviteButton.setEnabled(true);
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.registrationStartDate.setOnClickListener(v -> showDateTimePicker(binding.registrationStartDate));
        binding.registrationEndDate.setOnClickListener(v -> showDateTimePicker(binding.registrationEndDate));

        binding.cancelEventCreation.setOnClickListener(v -> {
            if (editEvent) {
                Bundle args = new Bundle();
                args.putSerializable("UserType", UserRole.ORGANIZER);
                args.putString("eventId", eventID);
                args.putBoolean("cameFromYourEvents", cameFromYourEvents);
                Navigation.findNavController(v).navigate(R.id.action_createEventFragment_to_eventDetailsFragment, args);
            } else {
                Bundle args = new Bundle();
                args.putBoolean("yourEvents", true);
                Navigation.findNavController(v).navigate(R.id.action_createEventFragment_to_browseEventsFragment, args);
            }
        });

        binding.createEvent.setOnClickListener(v -> createEventAndSave());
    }

    private void showDateTimePicker(EditText editText) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText("Select Time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utcCalendar.setTimeInMillis(selection);

                Calendar calendar = Calendar.getInstance();
                calendar.set(utcCalendar.get(Calendar.YEAR), 
                            utcCalendar.get(Calendar.MONTH), 
                            utcCalendar.get(Calendar.DAY_OF_MONTH),
                            timePicker.getHour(), 
                            timePicker.getMinute(), 
                            0);
                calendar.set(Calendar.MILLISECOND, 0);

                SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ENGLISH);
                editText.setText(sdf.format(calendar.getTime()));
            });

            timePicker.show(getChildFragmentManager(), "TIME_PICKER");
        });

        datePicker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void updatePreviews() {
        String coOrgText = "Co-Organizers added: " + coOrganizerIds.size();
        if (!coOrganizerNames.isEmpty()) {
            coOrgText += " [" + TextUtils.join(", ", coOrganizerNames) + "]";
        }
        binding.coOrganizersPreview.setText(coOrgText);

        String guestText = "Guests invited: " + invitedUserIds.size();
        if (!invitedUserNames.isEmpty()) {
            guestText += " [" + TextUtils.join(", ", invitedUserNames) + "]";
        }
        binding.invitedGuestsPreview.setText(guestText);
    }

    private void loadEventForEditing() {
        if (TextUtils.isEmpty(eventID)) {
            return;
        }

        new EventRepository().getEventById(eventID, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded()) {
                    return;
                }
                existingPosterUrl = event.getPosterUrl();
                binding.nameEventCreate.setText(event.getTitle());
                binding.eventDescriptionText.setText(event.getDescription());
                binding.registrationStartDate.setText(formatMillisForInput(event.getRegistrationStartMillis()));
                binding.registrationEndDate.setText(formatMillisForInput(event.getRegistrationEndMillis()));
                binding.maxEntrantsEditText.setText(String.valueOf(event.getCapacity()));

                invitedUserIds.clear();
                invitedUserIds.addAll(event.getInvitedUserIds());
                coOrganizerIds.clear();
                coOrganizerIds.addAll(event.getCoOrganizerIds());
                
                // For editing, we'd ideally fetch names, but for now we use IDs as placeholders
                invitedUserNames.clear();
                invitedUserNames.addAll(invitedUserIds);
                coOrganizerNames.clear();
                coOrganizerNames.addAll(coOrganizerIds);

                binding.privateEventSwitch.setChecked(event.isPrivate());
                binding.invitedGuestsLayout.setVisibility(event.isPrivate() ? View.VISIBLE : View.GONE);
                updatePreviews();

                binding.enableGeolocationSwitch.setChecked(event.isGeolocationRequired());

                if (!existingPosterUrl.isEmpty()) {
                    Glide.with(CreateEventFragment.this)
                            .load(existingPosterUrl)
                            .centerCrop()
                            .into(binding.imageButton);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load event for editing", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createEventAndSave() {
        String title = binding.nameEventCreate.getText().toString().trim();
        String description = binding.eventDescriptionText.getText().toString().trim();
        String startText = binding.registrationStartDate.getText().toString().trim();
        String endText = binding.registrationEndDate.getText().toString().trim();
        String capacityText = binding.maxEntrantsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description)
                || TextUtils.isEmpty(startText) || TextUtils.isEmpty(endText)) {
            Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        long registrationStartMillis;
        long registrationEndMillis;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ENGLISH);
            sdf.setLenient(false);
            registrationStartMillis = sdf.parse(startText).getTime();
            registrationEndMillis = sdf.parse(endText).getTime();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Please enter a valid date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (registrationEndMillis <= registrationStartMillis) {
            Toast.makeText(getContext(), "Registration end must be after registration start", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity = Event.DEFAULT_CAPACITY;
        if (!capacityText.isEmpty()) {
            try {
                capacity = Integer.parseInt(capacityText);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Maximum entrants must be a whole number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (capacity <= 0) {
                Toast.makeText(getContext(), "Maximum entrants must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String eventId = editEvent && !TextUtils.isEmpty(eventID)
                ? eventID
                : buildEventId(title);

        boolean isPrivate = binding.privateEventSwitch.isChecked();
        boolean geolocationRequired = binding.enableGeolocationSwitch.isChecked();

        String organizerId = "";
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            organizerId = firebaseUser.getUid();
        } else {
            Profile profile = new ProfileRepository(requireContext()).getProfile();
            if (profile != null) {
                organizerId = profile.getUid();
            }
        }

        if (TextUtils.isEmpty(organizerId)) {
            Toast.makeText(getContext(), "Error: Could not identify organizer. Please ensure you are logged in.", Toast.LENGTH_LONG).show();
            return;
        }

        // According to requirement: we become co-organizer or guest on ACCEPTANCE.
        // So during creation, we only save the organizer.
        // HOWEVER, for private events, people MUST be in invitedUserIds to even see it.
        // So we add guests to invitedUserIds immediately, but co-organizers will be added on acceptance.
        
        Event event = new Event(
                eventId,
                title,
                description,
                registrationStartMillis,
                registrationEndMillis,
                capacity,
                existingPosterUrl,
                isPrivate,
                invitedUserIds, // Guests allowed to see private event
                new ArrayList<>(), // No co-organizers yet
                organizerId,
                geolocationRequired
        );

        binding.loading.setVisibility(View.VISIBLE);
        binding.createEvent.setEnabled(false);

        if (selectedPosterUri != null) {
            uploadPosterAndSave(event, registrationEndMillis);
        } else {
            saveEventToFirestore(event, registrationEndMillis, existingPosterUrl);
        }
    }

    private void uploadPosterAndSave(Event event, long registrationEndMillis) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("event_posters")
                .child(event.getEventId() + ".jpg");

        ref.putFile(selectedPosterUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri -> saveEventToFirestore(event, registrationEndMillis, uri.toString()))
                                .addOnFailureListener(this::handleSaveError))
                .addOnFailureListener(this::handleSaveError);
    }

    private void saveEventToFirestore(Event event, long registrationEndMillis, @Nullable String posterUrl) {
        new EventRepository().createEvent(event, posterUrl, new EventRepository.CompletionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }

                binding.loading.setVisibility(View.GONE);
                binding.createEvent.setEnabled(true);
                
                // Send invitations now that event is created
                sendInvitations(event);

                long delay = registrationEndMillis - System.currentTimeMillis();
                Data inputData = new Data.Builder()
                        .putString("eventId", event.getEventId())
                        .build();

                OneTimeWorkRequest drawRequest =
                        new OneTimeWorkRequest.Builder(DrawEntrantsWorker.class)
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .setInputData(inputData)
                                .build();
                WorkManager.getInstance(requireContext()).enqueue(drawRequest);

                Toast.makeText(getContext(), editEvent ? "Event updated successfully" : "Event created successfully", Toast.LENGTH_SHORT).show();

                Bundle args = new Bundle();
                args.putString("eventId", event.getEventId());
                args.putSerializable("UserType", UserRole.ORGANIZER);
                args.putBoolean("cameFromYourEvents", true);
                Navigation.findNavController(requireView()).navigate(R.id.action_createEventFragment_to_eventDetailsFragment, args);
            }

            @Override
            public void onError(Exception e) {
                handleSaveError(e);
            }
        });
    }
    
    private void sendInvitations(Event event) {
        // Invite co-organizers - use specific type for acceptance flow
        for (String coOrgId : coOrganizerIds) {
            notificationService.sendCoOrganizerNotification(event, coOrgId, null);
        }
        
        // Invite private guests if applicable
        if (event.isPrivate()) {
            for (String guestId : invitedUserIds) {
                // Use a specific type for private invites
                sendPrivateInviteNotification(event, guestId);
            }
        }
    }

    private void sendPrivateInviteNotification(Event event, String userId) {
        long nowMillis = System.currentTimeMillis();
        String message = "You have been invited to join the private event: " + event.getTitle();
        
        com.example.releasethekraken.model.Notification notification = new com.example.releasethekraken.model.Notification(
                userId,
                event.getEventId(),
                message,
                "PRIVATE_INVITE",
                nowMillis
        );
        
        new NotificationRepository().sendNotification(notification, new NotificationRepository.CompletionCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(Exception e) {}
        });
    }

    private void handleSaveError(Exception e) {
        if (!isAdded()) {
            return;
        }
        binding.loading.setVisibility(View.GONE);
        binding.createEvent.setEnabled(true);
        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private String buildEventId(String title) {
        String normalizedTitle = title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalizedTitle.isEmpty()) {
            normalizedTitle = "event";
        }
        return normalizedTitle + "_" + System.currentTimeMillis();
    }

    private String formatMillisForInput(long millis) {
        return new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ENGLISH).format(millis);
    }
}