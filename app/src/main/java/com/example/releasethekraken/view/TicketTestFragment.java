package com.example.releasethekraken.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.AccessControl;
import com.example.releasethekraken.controller.EventFilterService;
import com.example.releasethekraken.controller.EventHistoryService;
import com.example.releasethekraken.controller.SampleDataRepository;
import com.example.releasethekraken.controller.SessionManager;
import com.example.releasethekraken.databinding.FragmentTicketTestBinding;
import com.example.releasethekraken.model.EventHistoryEntry;
import com.example.releasethekraken.model.Feature;
import com.example.releasethekraken.model.FilterEvent;
import com.example.releasethekraken.model.UserRole;

public class TicketTestFragment extends Fragment {
    private static final String CURRENT_ENTRANT_ID = "entrant_device_1";

    private FragmentTicketTestBinding binding;
    private SessionManager sessionManager;
    private UserRole activeRole;

    private List<FilterEvent> eventCatalog;
    private List<EventHistoryEntry> allHistoryEntries;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentTicketTestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        activeRole = sessionManager.getRole();
        eventCatalog = SampleDataRepository.loadEvents();
        allHistoryEntries = SampleDataRepository.loadEventHistory();

        binding.backToMenuButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_ticketTestFragment_to_mainMenuFragment)
        );

        setupRolePicker();
        setupFeatureButtons();
        updateRoleSummary();
    }

    private void setupRolePicker() {
        UserRole[] roles = UserRole.values();
        String[] labels = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            labels[i] = roles[i].getLabel();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.roleSpinner.setAdapter(adapter);
        binding.roleSpinner.setSelection(activeRole.ordinal());
        binding.roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                activeRole = roles[position];
                sessionManager.setRole(activeRole);
                updateRoleSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupFeatureButtons() {
        wireFeatureButton(binding.btnJoinWaitingList, Feature.JOIN_WAITING_LIST);
        wireFilterEventsButton();
        wireViewHistoryButton();

        wireFeatureButton(binding.btnCreateEvent, Feature.CREATE_EVENT);
        wireFeatureButton(binding.btnViewEntrants, Feature.VIEW_ENTRANTS);
        wireFeatureButton(binding.btnDrawLottery, Feature.DRAW_LOTTERY);

        wireFeatureButton(binding.btnBrowseEvents, Feature.BROWSE_EVENTS);
        wireFeatureButton(binding.btnRemoveEvent, Feature.REMOVE_EVENT);
        wireFeatureButton(binding.btnRemoveProfile, Feature.REMOVE_PROFILE);
        wireFeatureButton(binding.btnReviewLogs, Feature.REVIEW_NOTIFICATION_LOGS);
    }

    private void wireFeatureButton(View button, Feature feature) {
        button.setOnClickListener(v -> {
            if (AccessControl.canAccess(activeRole, feature)) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.ticket_test_access_granted, feature.getLabel(), activeRole.getLabel()),
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.ticket_test_access_denied, activeRole.getLabel(), feature.getLabel()),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void wireFilterEventsButton() {
        binding.btnFilterEvents.setOnClickListener(v -> {
            if (!AccessControl.canAccess(activeRole, Feature.FILTER_EVENTS)) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.ticket_test_access_denied, activeRole.getLabel(), Feature.FILTER_EVENTS.getLabel()),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Set<String> interests = parseCsv(binding.inputInterests.getText().toString());
            Set<String> availability = parseCsv(binding.inputAvailability.getText().toString());
            List<FilterEvent> filtered = EventFilterService.filterByInterestsAndAvailability(
                    eventCatalog,
                    interests,
                    availability
            );
            binding.filterResultsText.setText(formatFilterResults(filtered));
        });
    }

    private void wireViewHistoryButton() {
        binding.btnViewEventHistory.setOnClickListener(v -> {
            if (!AccessControl.canAccess(activeRole, Feature.VIEW_EVENT_HISTORY)) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.ticket_test_access_denied, activeRole.getLabel(), Feature.VIEW_EVENT_HISTORY.getLabel()),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            List<EventHistoryEntry> history = EventHistoryService.getHistoryForEntrant(
                    allHistoryEntries,
                    CURRENT_ENTRANT_ID
            );
            binding.historyResultsText.setText(formatHistoryResults(history));
        });
    }

    private Set<String> parseCsv(String value) {
        Set<String> output = new HashSet<>();
        if (value == null || value.trim().isEmpty()) {
            return output;
        }
        String[] tokens = value.split(",");
        for (String token : tokens) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                output.add(normalized);
            }
        }
        return output;
    }

    private String formatFilterResults(List<FilterEvent> events) {
        if (events.isEmpty()) {
            return getString(R.string.ticket_test_no_filter_results);
        }
        StringBuilder builder = new StringBuilder();
        for (FilterEvent event : events) {
            builder.append("- ")
                    .append(event.getTitle())
                    .append(" (")
                    .append(capitalize(event.getDay()))
                    .append(")\n");
        }
        return builder.toString().trim();
    }

    private String formatHistoryResults(List<EventHistoryEntry> historyEntries) {
        if (historyEntries.isEmpty()) {
            return getString(R.string.ticket_test_no_history_results);
        }
        StringBuilder builder = new StringBuilder();
        for (EventHistoryEntry entry : historyEntries) {
            builder.append("- ")
                    .append(entry.getEventTitle())
                    .append(" - ")
                    .append(entry.getOutcome().getLabel())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private void updateRoleSummary() {
        binding.roleSummaryText.setText(getString(R.string.ticket_test_active_role, activeRole.getLabel()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
