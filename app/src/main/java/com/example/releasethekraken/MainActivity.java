package com.example.releasethekraken;

import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String CURRENT_ENTRANT_ID = "entrant_device_1";

    private SessionManager sessionManager;
    private UserRole activeRole;
    private TextView roleSummaryText;
    private EditText interestsInput;
    private EditText availabilityInput;
    private TextView filterResultsText;
    private TextView historyResultsText;

    private List<Event> eventCatalog;
    private List<EventHistoryEntry> allHistoryEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        activeRole = sessionManager.getRole();
        eventCatalog = SampleDataRepository.loadEvents();
        allHistoryEntries = SampleDataRepository.loadEventHistory();

        roleSummaryText = findViewById(R.id.roleSummaryText);
        interestsInput = findViewById(R.id.inputInterests);
        availabilityInput = findViewById(R.id.inputAvailability);
        filterResultsText = findViewById(R.id.filterResultsText);
        historyResultsText = findViewById(R.id.historyResultsText);
        setupRolePicker();
        setupFeatureButtons();
        updateRoleSummary();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupRolePicker() {
        Spinner roleSpinner = findViewById(R.id.roleSpinner);
        UserRole[] roles = UserRole.values();
        String[] labels = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            labels[i] = roles[i].getLabel();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
        roleSpinner.setSelection(activeRole.ordinal());
        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        wireFeatureButton(R.id.btnJoinWaitingList, Feature.JOIN_WAITING_LIST);
        wireFilterEventsButton();
        wireViewHistoryButton();

        wireFeatureButton(R.id.btnCreateEvent, Feature.CREATE_EVENT);
        wireFeatureButton(R.id.btnViewEntrants, Feature.VIEW_ENTRANTS);
        wireFeatureButton(R.id.btnDrawLottery, Feature.DRAW_LOTTERY);

        wireFeatureButton(R.id.btnBrowseEvents, Feature.BROWSE_EVENTS);
        wireFeatureButton(R.id.btnRemoveEvent, Feature.REMOVE_EVENT);
        wireFeatureButton(R.id.btnRemoveProfile, Feature.REMOVE_PROFILE);
        wireFeatureButton(R.id.btnReviewLogs, Feature.REVIEW_NOTIFICATION_LOGS);
    }

    private void wireFeatureButton(int buttonId, Feature feature) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(v -> {
            if (AccessControl.canAccess(activeRole, feature)) {
                Toast.makeText(
                        this,
                        getString(R.string.access_granted, feature.getLabel(), activeRole.getLabel()),
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        this,
                        getString(R.string.access_denied, activeRole.getLabel(), feature.getLabel()),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void wireFilterEventsButton() {
        Button button = findViewById(R.id.btnFilterEvents);
        button.setOnClickListener(v -> {
            if (!AccessControl.canAccess(activeRole, Feature.FILTER_EVENTS)) {
                Toast.makeText(
                        this,
                        getString(R.string.access_denied, activeRole.getLabel(), Feature.FILTER_EVENTS.getLabel()),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Set<String> interests = parseCsv(interestsInput.getText().toString());
            Set<String> availability = parseCsv(availabilityInput.getText().toString());
            List<Event> filtered = EventFilterService.filterByInterestsAndAvailability(eventCatalog, interests, availability);
            filterResultsText.setText(formatFilterResults(filtered));
        });
    }

    private void wireViewHistoryButton() {
        Button button = findViewById(R.id.btnViewEventHistory);
        button.setOnClickListener(v -> {
            if (!AccessControl.canAccess(activeRole, Feature.VIEW_EVENT_HISTORY)) {
                Toast.makeText(
                        this,
                        getString(R.string.access_denied, activeRole.getLabel(), Feature.VIEW_EVENT_HISTORY.getLabel()),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            List<EventHistoryEntry> history = EventHistoryService.getHistoryForEntrant(allHistoryEntries, CURRENT_ENTRANT_ID);
            historyResultsText.setText(formatHistoryResults(history));
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

    private String formatFilterResults(List<Event> events) {
        if (events.isEmpty()) {
            return getString(R.string.no_filter_results);
        }
        StringBuilder builder = new StringBuilder();
        for (Event event : events) {
            builder.append("• ")
                    .append(event.getTitle())
                    .append(" (")
                    .append(capitalize(event.getDay()))
                    .append(")\n");
        }
        return builder.toString().trim();
    }

    private String formatHistoryResults(List<EventHistoryEntry> historyEntries) {
        if (historyEntries.isEmpty()) {
            return getString(R.string.no_history_results);
        }
        StringBuilder builder = new StringBuilder();
        for (EventHistoryEntry entry : historyEntries) {
            builder.append("• ")
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
        roleSummaryText.setText(getString(R.string.active_role, activeRole.getLabel()));
    }
}
