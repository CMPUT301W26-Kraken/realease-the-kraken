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

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.FragmentCreateEventBinding;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;

public class CreateEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;

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

        // Navigate back to browse events
        binding.cancelEventCreation.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_createEventFragment_to_browseEventsFragment)
        );

        // Create event and save to Firestore
        binding.createEvent.setOnClickListener(v -> createEventAndSave());
    }

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