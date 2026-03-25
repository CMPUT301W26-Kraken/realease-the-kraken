package com.example.releasethekraken.view;

import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

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
 * This is the fragment that is used to create events and add them to the repository. It is also
 * used in editing information about events and updating that information in the repository.
 *
 * It takes three arguments from the navigator when this event is navigated to;
 * Boolean editEvent that is true when editing the events and false when not editing the events and
 *  is used to adjust UI elements and update the event instead of creating a new one.
 * Boolean cameFromYourEvents that determines if the user entered the event creator via your events
 *  or browse events. It is true when you have come from your events and is used in backwards navigability.
 * String eventID contains the string version of the event ID and is used when the event is chosen to be
 *  edited so that it can properly prefill the relevant fields to be edited.
 */
public class CreateEventFragment extends Fragment {
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy h:mm a";
    private static final long MAX_POSTER_PREVIEW_BYTES = 5L * 1024L * 1024L;

    private FragmentCreateEventBinding binding;
    private boolean editEvent, cameFromYourEvents;
    private String eventID;
    private Uri selectedPosterUri;
    private String existingPosterUrl = "";

    private final ActivityResultLauncher<String> posterPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || binding == null) {
                    return;
                }
                selectedPosterUri = uri;
                applyLocalPosterPreview(uri);
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
            loadEventForEditing();
        }

        binding.imageButton.setOnClickListener(v -> posterPickerLauncher.launch("image/*"));
        binding.uploadPosterText.setOnClickListener(v -> posterPickerLauncher.launch("image/*"));

        // Navigate back to main menu
        binding.cancelEventCreation.setOnClickListener(v -> {
            if (editEvent) {
                // An existing event had its edits cancelled so we return to its details page
                Bundle args = new Bundle();
                args.putSerializable("UserType", UserRole.ORGANIZER);
                args.putString("eventId", eventID);
                args.putBoolean("cameFromYourEvents", cameFromYourEvents);
                Navigation.findNavController(v).navigate(R.id.action_createEventFragment_to_eventDetailsFragment, args);
            } else {
                // A new event being created was canceled, so the user had to have come from your events
                Bundle args = new Bundle();
                args.putBoolean("yourEvents", true);
                Navigation.findNavController(v).navigate(R.id.action_createEventFragment_to_browseEventsFragment, args);
            }
        });

        // Create event and save to Firestore
        binding.createEvent.setOnClickListener(v -> createEventAndSave());
    }

    /**
     * The method that is called to handle event creations based on the information in the fields
     * and writes the event into the repository and the database. Sets up a worker to trigger on
     * registration closing
     */
    private void createEventAndSave() {
        // Pull the raw user input first so validation can happen before any repository work.
        String title = binding.nameEventCreate.getText().toString().trim();
        String description = binding.eventDescriptionText.getText().toString().trim();
        String startText = binding.registrationStartDate.getText().toString().trim();
        String endText = binding.registrationEndDate.getText().toString().trim();
        String capacityText = binding.maxEntrantsEditText.getText().toString().trim();

        // Capacity is optional, but the rest of the event data is required to create a usable
        // browseable event in Firestore.
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(startText) || TextUtils.isEmpty(endText)) {
            Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        long registrationStartMillis, registrationEndMillis;
        try {
            // Keep create-event input and browse-event filter input on the same date format so
            // the user only has to learn one timestamp convention in the app.
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ENGLISH);
            sdf.setLenient(false);
            registrationStartMillis = sdf.parse(startText).getTime();
            registrationEndMillis = sdf.parse(endText).getTime();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Enter dates as dd/MM/yyyy h:mm AM/PM", Toast.LENGTH_SHORT).show();
            return;
        }

        // Registration end drives worker scheduling, so reject inverted windows before saving.
        if (registrationEndMillis <= registrationStartMillis) {
            Toast.makeText(getContext(), "Registration end must be after registration start", Toast.LENGTH_SHORT).show();
            return;
        }

        // Blank capacity means "use the app default". Any provided value must be a positive whole
        // number because browse-time filtering compares numeric capacities.
        int capacity = Event.DEFAULT_CAPACITY;
        if (!capacityText.isEmpty()) {
            try {
                capacity = Integer.parseInt(capacityText);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(),
                        "Maximum entrants must be a whole number",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (capacity <= 0) {
                Toast.makeText(getContext(),
                        "Maximum entrants must be greater than zero",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Disable the button while Firestore writes so duplicate taps do not create duplicate
        // events or enqueue multiple draw workers.
        binding.loading.setVisibility(View.VISIBLE);
        binding.createEvent.setEnabled(false);

        String eventId = editEvent && !TextUtils.isEmpty(eventID)
                ? eventID
                : buildUniqueEventId(title);

        if (selectedPosterUri != null) {
            uploadPosterAndSaveEvent(
                    eventId,
                    title,
                    description,
                    registrationStartMillis,
                    registrationEndMillis,
                    capacity
            );
            return;
        }

        persistEvent(
                eventId,
                title,
                description,
                registrationStartMillis,
                registrationEndMillis,
                capacity,
                existingPosterUrl
        );
    }

    private void uploadPosterAndSaveEvent(
            String eventId,
            String title,
            String description,
            long registrationStartMillis,
            long registrationEndMillis,
            int capacity
    ) {
        StorageReference posterRef = FirebaseStorage.getInstance()
                .getReference()
                .child("event_posters")
                .child(eventId + ".jpg");

        posterRef.putFile(selectedPosterUri)
                .addOnSuccessListener(taskSnapshot -> posterRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> persistEvent(
                                eventId,
                                title,
                                description,
                                registrationStartMillis,
                                registrationEndMillis,
                                capacity,
                                downloadUri.toString()
                        ))
                        .addOnFailureListener(this::handleSaveError))
                .addOnFailureListener(this::handleSaveError);
    }

    private void persistEvent(
            String eventId,
            String title,
            String description,
            long registrationStartMillis,
            long registrationEndMillis,
            int capacity,
            String posterUrl
    ) {
        Event event = new Event(
                eventId,
                title,
                description,
                registrationStartMillis,
                registrationEndMillis,
                capacity,
                posterUrl
        );

        new EventRepository().createEvent(event, new EventRepository.CompletionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;

                binding.loading.setVisibility(View.GONE);
                binding.createEvent.setEnabled(true);

                // Worker delay is based on the registration close time. This keeps entrant drawing
                // aligned with the same registration window used by browse-event availability
                // filtering.
                long delay = registrationEndMillis - System.currentTimeMillis();

                // Pass the event id into the worker so the background draw job knows which event
                // should be processed once registration closes.
                Data inputData = new Data.Builder()
                        .putString("eventId", eventId)
                        .build();

                // Queue a one-time worker instead of blocking the UI thread or relying on the
                // fragment still being alive when registration closes.
                OneTimeWorkRequest drawRequest =
                        new OneTimeWorkRequest.Builder(DrawEntrantsWorker.class)
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .setInputData(inputData)
                                .build();
                WorkManager.getInstance(requireContext()).enqueue(drawRequest);

                Toast.makeText(getContext(), "Event created successfully", Toast.LENGTH_SHORT).show();

                // Navigate to details instead of browse
                Bundle args = new Bundle();
                args.putString("eventId", eventId);
                args.putSerializable("UserType", UserRole.ORGANIZER);
                args.putBoolean("showQrOnLoad", true);
                Navigation.findNavController(requireView()).navigate(R.id.action_createEventFragment_to_eventDetailsFragment, args);
            }

            @Override
            public void onError(Exception e) {
                handleSaveError(e);
            }
        });
    }

    private void handleSaveError(Exception e) {
        if (!isAdded()) return;
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
                loadPosterPreview(existingPosterUrl);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "Failed to load event for editing", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildUniqueEventId(String title) {
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

    private void applyLocalPosterPreview(Uri uri) {
        binding.imageButton.setImageTintList((ColorStateList) null);
        binding.imageButton.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        binding.imageButton.setImageURI(uri);
    }

    private void loadPosterPreview(String posterUrl) {
        if (TextUtils.isEmpty(posterUrl)) {
            return;
        }

        FirebaseStorage.getInstance()
                .getReferenceFromUrl(posterUrl)
                .getBytes(MAX_POSTER_PREVIEW_BYTES)
                .addOnSuccessListener(bytes -> {
                    if (binding == null) {
                        return;
                    }
                    binding.imageButton.setImageTintList((ColorStateList) null);
                    binding.imageButton.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    binding.imageButton.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Unable to load poster preview", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
