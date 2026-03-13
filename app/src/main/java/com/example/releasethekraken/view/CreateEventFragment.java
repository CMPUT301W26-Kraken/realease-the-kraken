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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.releasethekraken.MainActivity;
import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.DrawEntrantsWorker;
import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.databinding.FragmentCreateEventBinding;
import com.example.releasethekraken.databinding.FragmentViewProfileBinding;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;

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
        String title = binding.nameEventCreate.getText().toString().trim();
        String description = binding.eventDescriptionText.getText().toString().trim();
        String startText = binding.registrationStartDate.getText().toString().trim();
        String endText = binding.registrationEndDate.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description)
                || TextUtils.isEmpty(startText) || TextUtils.isEmpty(endText)) {
            Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        long registrationStartMillis;
        long registrationEndMillis;

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
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
                    "Enter dates as dd/MM/yyyy",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (registrationEndMillis < registrationStartMillis) {
            Toast.makeText(getContext(),
                    "Registration end must be after registration start",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String eventId = title.replaceAll("\\s+", "_").toLowerCase();

        Event event = new Event(
                eventId,
                title,
                description,
                registrationStartMillis,
                registrationEndMillis
        );

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

                //Ethan here adding some epic code that starts a timer to check registration ending!
                long delay = registrationEndMillis - System.currentTimeMillis(); //Gets the delay from now until the reg closes

                //getting our event id for the worker so that we know which event we are waiting to draw from
                Data inputData = new Data.Builder()
                        .putString("eventId", eventId)
                        .build();

                //making our request with DrawEntrantsWorker to draw entrants when delay expires
                OneTimeWorkRequest drawRequest =
                        new OneTimeWorkRequest.Builder(DrawEntrantsWorker.class)
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS) //tells it to wait "delay" long until running the worker
                                .setInputData(inputData) //store that juicy data to give to DrawEntrantsWorker
                                .build();

                WorkManager.getInstance(requireContext()).enqueue(drawRequest); //submit that job! LETS GOOO
                //End of Ethan's very cool code

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