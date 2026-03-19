package com.example.releasethekraken.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private FragmentCreateEventBinding binding;
    private boolean editEvent, cameFromYourEvents;
    private String eventID;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }

        if (editEvent == true) {
            binding.eventCreateWelcome.setText(R.string.edit_event_welcome);
            binding.createEvent.setText(R.string.edit_event_confirm_button);
            //TODO: FILL IN REMAINING FIELDS WITH EXISTING EVENTS INFORMATION
        }

        // Navigate back to main menu
        binding.cancelEventCreation.setOnClickListener(v -> {
            if (editEvent) {
                // An existing event had its edits cancelled so we return to its details page
                Bundle args = new Bundle();
                args.putSerializable("UserType", UserRole.ORGANIZER);
                args.putString("eventId", eventID);
                args.putBoolean("cameFromYourEvents", cameFromYourEvents);

                Navigation.findNavController(v)
                        .navigate(R.id.action_createEventFragment_to_eventDetailsFragment, args);
            } else {
                // A new event being created was canceled, so the user had to have come from your events
                Bundle args = new Bundle();
                args.putBoolean("yourEvents", true);

                Navigation.findNavController(v)
                        .navigate(R.id.action_createEventFragment_to_browseEventsFragment, args);
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
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description)
                || TextUtils.isEmpty(startText) || TextUtils.isEmpty(endText)) {
            Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        long registrationStartMillis;
        long registrationEndMillis;

        try {
            // Keep create-event input and browse-event filter input on the same date format so
            // the user only has to learn one timestamp convention in the app.
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy h:mm a", java.util.Locale.ENGLISH);
            sdf.setLenient(false);

            java.util.Date startDate = sdf.parse(startText);
            java.util.Date endDate = sdf.parse(endText);

            if (startDate == null || endDate == null) {
                Toast.makeText(getContext(), "Invalid date format", Toast.LENGTH_SHORT).show();
                return;
            }

            registrationStartMillis = startDate.getTime();
            registrationEndMillis = endDate.getTime();
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Enter dates as dd/MM/yyyy h:mm AM/PM",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Registration end drives worker scheduling, so reject inverted windows before saving.
        if (registrationEndMillis < registrationStartMillis) {
            Toast.makeText(getContext(),
                    "Registration end must be after registration start",
                    Toast.LENGTH_SHORT).show();
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

        // Current app flow still derives an id from the title. Not ideal long-term, but kept
        // unchanged here so the new filtering work does not alter navigation semantics.
        String eventId = title.replaceAll("\\s+", "_").toLowerCase();

        Event event = new Event(
                eventId,
                title,
                description,
                registrationStartMillis,
                registrationEndMillis,
                capacity
        );

        // Disable the button while Firestore writes so duplicate taps do not create duplicate
        // events or enqueue multiple draw workers.
        binding.loading.setVisibility(View.VISIBLE);
        binding.createEvent.setEnabled(false);

        EventRepository repository = new EventRepository();
        repository.createEvent(event, new EventRepository.CompletionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }

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

                Navigation.findNavController(requireView())
                        .navigate(R.id.action_createEventFragment_to_browseEventsFragment);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) {
                    return;
                }

                binding.loading.setVisibility(View.GONE);
                binding.createEvent.setEnabled(true);

                Toast.makeText(getContext(),
                        "Failed to create event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
