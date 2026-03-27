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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Creates new events and edits existing ones.
 * Poster upload/update is handled here so organizers can attach or replace event images.
 */
public class CreateEventFragment extends Fragment {
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy h:mm a";

    private FragmentCreateEventBinding binding;
    private boolean editEvent;
    private boolean cameFromYourEvents;
    private String eventID;
    private Uri selectedPosterUri;
    private String existingPosterUrl = "";

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
        }

        binding.imageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.uploadPosterText.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

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

        String eventId = editEvent && !TextUtils.isEmpty(eventID)
                ? eventID
                : buildEventId(title);

        Event event = new Event(
                eventId,
                title,
                description,
                registrationStartMillis,
                registrationEndMillis,
                capacity,
                existingPosterUrl
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

    private void handleSaveError(Exception e) {
        if (!isAdded()) {
            return;
        }
        binding.loading.setVisibility(View.GONE);
        binding.createEvent.setEnabled(true);
        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(requireContext(), "Failed to load event for editing", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
