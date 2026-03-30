package com.example.releasethekraken.view;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.releasethekraken.databinding.FragmentCreateEventBinding;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.repository.ProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class CreateEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;
    private boolean editEvent, cameFromYourEvents;
    private String eventID;

    private Uri selectedPosterUri = null;

    // store invited users (IDs)
    private final ArrayList<String> invitedUserIds = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPosterUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(binding.imageButton);
                }
            });

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) activity.getSupportActionBar().show();
        }

        if (editEvent) {
            binding.eventCreateWelcome.setText(R.string.edit_event_welcome);
            binding.createEvent.setText(R.string.edit_event_confirm_button);
            loadEventToEdit();
        }

        binding.imageButton.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*")
        );

        //private switch logic
        binding.privateEventSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.inviteSectionTitle.setVisibility(View.VISIBLE);
                binding.inviteSearchInput.setVisibility(View.VISIBLE);
                binding.addInviteButton.setVisibility(View.VISIBLE);
                binding.invitedUsersPreview.setVisibility(View.VISIBLE);
            } else {
                binding.inviteSectionTitle.setVisibility(View.GONE);
                binding.inviteSearchInput.setVisibility(View.GONE);
                binding.addInviteButton.setVisibility(View.GONE);
                binding.invitedUsersPreview.setVisibility(View.GONE);
            }
        });

        // INVITE BUTTON searching for real users (EMAIL HAS BEEN THE BEST WORKING)
        binding.addInviteButton.setOnClickListener(v -> {
            String input = binding.inviteSearchInput.getText().toString().trim();

            if (TextUtils.isEmpty(input)) {
                Toast.makeText(getContext(), "Enter a name, email, or phone to invite", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.addInviteButton.setEnabled(false);
            new ProfileRepository(requireContext())
                    .searchProfiles(input, new ProfileRepository.ProfileRepositoryCallback<com.example.releasethekraken.model.Profile>() {
                        @Override
                        public void onSuccess(com.example.releasethekraken.model.Profile profile) {
                            if (!isAdded()) return;
                            binding.addInviteButton.setEnabled(true);
                            if (invitedUserIds.contains(profile.getUid())) {
                                Toast.makeText(getContext(), "User already invited", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            invitedUserIds.add(profile.getUid());
                            binding.inviteSearchInput.setText("");
                            binding.invitedUsersPreview.setText("Invited: " + invitedUserIds.size() + " (" + profile.getName() + ")");
                            Toast.makeText(getContext(), "Added " + profile.getName(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            if (!isAdded()) return;
                            binding.addInviteButton.setEnabled(true);
                            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

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

    private void loadEventToEdit() {
        if (eventID == null) return;
        new EventRepository().getEventById(eventID, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded()) return;
                binding.nameEventCreate.setText(event.getTitle());
                binding.eventDescriptionText.setText(event.getDescription());
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy h:mm a", java.util.Locale.ENGLISH);
                binding.registrationStartDate.setText(sdf.format(new java.util.Date(event.getRegistrationStartMillis())));
                binding.registrationEndDate.setText(sdf.format(new java.util.Date(event.getRegistrationEndMillis())));
                binding.maxEntrantsEditText.setText(String.valueOf(event.getCapacity()));
                binding.privateEventSwitch.setChecked(event.isPrivate());
                invitedUserIds.clear();
                invitedUserIds.addAll(event.getInvitedUserIds());
                binding.invitedUsersPreview.setText("Invited: " + invitedUserIds.size());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to load event for editing", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createEventAndSave() {
        String title = binding.nameEventCreate.getText().toString().trim();
        String description = binding.eventDescriptionText.getText().toString().trim();
        String startText = binding.registrationStartDate.getText().toString().trim();
        String endText = binding.registrationEndDate.getText().toString().trim();
        String capacityText = binding.maxEntrantsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(startText) || TextUtils.isEmpty(endText)) {
            Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        long registrationStartMillis, registrationEndMillis;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy h:mm a", java.util.Locale.ENGLISH);
            sdf.setLenient(false);
            registrationStartMillis = sdf.parse(startText).getTime();
            registrationEndMillis = sdf.parse(endText).getTime();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Enter dates as dd/MM/yyyy h:mm AM/PM", Toast.LENGTH_SHORT).show();
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

        String eventId = editEvent ? eventID : title.replaceAll("\\s+", "_").toLowerCase();

        // get private state
        boolean isPrivate = binding.privateEventSwitch.isChecked();

        // Use real organizer UID
        String organizerId = "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            organizerId = user.getUid();
        } else {
            // Fallback to local profile UID if available
            organizerId = new ProfileRepository(requireContext()).getProfile().getUid();
        }

        if (TextUtils.isEmpty(organizerId)) {
            Toast.makeText(getContext(), "Error: Could not identify organizer. Please ensure you are logged in.", Toast.LENGTH_LONG).show();
            return;
        }

        Event event = new Event(
                eventId,
                title,
                description,
                registrationStartMillis,
                registrationEndMillis,
                capacity,
                isPrivate,
                invitedUserIds,
                organizerId
        );

        binding.loading.setVisibility(View.VISIBLE);
        binding.createEvent.setEnabled(false);

        if (selectedPosterUri != null) {
            uploadPosterAndSave(event, eventId, registrationEndMillis);
        } else {
            saveEventToFirestore(event, eventId, registrationEndMillis, null);
        }
    }

    private void uploadPosterAndSave(Event event, String eventId, long registrationEndMillis) {
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("event_posters")
                .child(eventId + ".jpg");

        ref.putFile(selectedPosterUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        saveEventToFirestore(event, eventId, registrationEndMillis, uri.toString())
                                )
                                .addOnFailureListener(e ->
                                        saveEventToFirestore(event, eventId, registrationEndMillis, null)
                                ))
                .addOnFailureListener(e ->
                        saveEventToFirestore(event, eventId, registrationEndMillis, null)
                );
    }

    private void saveEventToFirestore(Event event, String eventId,
                                      long registrationEndMillis, @Nullable String posterImageUrl) {
        new EventRepository().createEvent(event, posterImageUrl, new EventRepository.CompletionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;

                binding.loading.setVisibility(View.GONE);
                binding.createEvent.setEnabled(true);

                long delay = registrationEndMillis - System.currentTimeMillis();

                Data inputData = new Data.Builder()
                        .putString("eventId", eventId)
                        .build();

                OneTimeWorkRequest drawRequest =
                        new OneTimeWorkRequest.Builder(DrawEntrantsWorker.class)
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .setInputData(inputData)
                                .build();

                WorkManager.getInstance(requireContext()).enqueue(drawRequest);

                Toast.makeText(getContext(), editEvent ? "Event updated successfully" : "Event created successfully", Toast.LENGTH_SHORT).show();

                Bundle args = new Bundle();
                args.putString("eventId", eventId);
                args.putSerializable("UserType", UserRole.ORGANIZER);
                Navigation.findNavController(requireView()).navigate(R.id.action_createEventFragment_to_eventDetailsFragment, args);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                binding.loading.setVisibility(View.GONE);
                binding.createEvent.setEnabled(true);
                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}