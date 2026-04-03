package com.example.releasethekraken.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.SnapHelper;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.EventFilterService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * BrowseEventsFragment acts as the fragment that displays lists of events and is used
 * to display all events in browse events, and only enrolled/created events in the main menu.
 *
 * Takes two arguments when the fragment is navigated to, the first is a column count that chooses
 * the amount of columns to display, and the second is a boolean called "yourEvents" which determines
 * if you are viewing it through your events and uses it to set UI elements accordingly.
 */
public class BrowseEventsFragment extends Fragment {
    private static final String ARG_YOUR_EVENTS = "yourEvents";
    // Keep browse-screen parsing aligned with create-event input so users enter one shared format.
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy h:mm a";

    private static final String ARG_COLUMN_COUNT = "column-count";
    private boolean yourEvents;

    // allEvents is the source-of-truth list from Firestore.
    // visibleEvents is the currently rendered subset after search/filter predicates are applied.
    // Keeping both lists avoids re-fetching from Firestore every time the user changes a filter.
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> visibleEvents = new ArrayList<>();
    private MyItemRecyclerViewAdapter adapter;
    private EditText searchEventsText;
    private EditText filterAvailableAtText;
    private EditText filterCapacityText;
    private TextView emptyResultsText;
    private LinearLayout filterBar;
    private LinearLayout filterButtons;
    private LinearSnapHelper snapHelper;

    public BrowseEventsFragment() { }

    public static BrowseEventsFragment newInstance(int columnCount) {
        BrowseEventsFragment fragment = new BrowseEventsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            yourEvents = getArguments().getBoolean(ARG_YOUR_EVENTS, false);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_browse_events, container, false);

        // The same fragment backs both "Browse Events" and "Your Events", so the title and create
        // button visibility are adjusted from the navigation argument instead of duplicating screens.
        TextView welcomeText = view.findViewById(R.id.welcome_text);
        if (yourEvents) {
            welcomeText.setText(getString(R.string.your_events_welcome));
        } else {
            welcomeText.setText(getString(R.string.browse_events_welcome));
        }

        // RecyclerView is populated from visibleEvents so the adapter only ever renders the
        // already-filtered data set.
        RecyclerView recyclerView = view.findViewById(R.id.events_recycler_view);

        searchEventsText = view.findViewById(R.id.search_events_text);
        filterAvailableAtText = view.findViewById(R.id.filter_available_at_text);
        filterCapacityText = view.findViewById(R.id.filter_capacity_text);
        emptyResultsText = view.findViewById(R.id.empty_results_text);

        filterBar = view.findViewById(R.id.browse_filter_layout);
        filterButtons = view.findViewById(R.id.browse_filter_button_layout);

        filterAvailableAtText.setOnClickListener(v -> showDateTimePicker(filterAvailableAtText));

        Button createEventButton = view.findViewById(R.id.create_event_button);
        Switch toggleDetailedView = view.findViewById(R.id.toggle_detailed_switch);
        // Hide create button during normal event browsing, and vice versa with the detailed mode switch
        if (!yourEvents) {
            createEventButton.setVisibility(View.GONE);
            toggleDetailedView.setVisibility(View.VISIBLE);
        } else {
            createEventButton.setVisibility(View.VISIBLE);
            toggleDetailedView.setVisibility(View.GONE);
        }

        // Navigate to Create Events
        createEventButton
                .setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putBoolean("editEvent", false);
                    args.putBoolean("cameFromYourEvents", true); // Set to true because it guaranteed means the user came from the your events page

                    Navigation.findNavController(v)
                            .navigate(R.id.action_browseEventsFragment_to_createEventFragment, args);
                });

        // Event taps still navigate using the existing event-details flow. Search/filtering only
        // changes which events are visible, not how selection/navigation works.
        adapter = new MyItemRecyclerViewAdapter(visibleEvents, event -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            // Private events remain visible in the list, but only invited users or the organizer
            // are allowed to open them.
            if (event.isPrivate()) {
                if (currentUser == null) {
                    Toast.makeText(getContext(),
                            "You must be logged in to access a private event",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String currentUserId = currentUser.getUid();
                boolean isOrganizer = event.getOrganizerId().equals(currentUserId);
                boolean isCoOrganizer = event.getCoOrganizerIds().contains(currentUserId);
                boolean isInvited = event.getInvitedUserIds().contains(currentUserId);

                if (!isOrganizer && !isCoOrganizer && !isInvited) {
                    Toast.makeText(getContext(),
                            "You are not invited to this private event",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Bundle args = new Bundle();
            args.putString(EventDetailsFragment.ARG_EVENT_ID, event.getEventId());
            args.putBoolean(EventDetailsFragment.ARG_IS_PRIVATE, event.isPrivate());

            // Pass UserRole.ENTRANT as default; EventDetailsFragment will re-calculate based on event ownership/co-organizer list
            args.putSerializable("UserType", UserRole.ENTRANT);

            args.putBoolean("cameFromYourEvents", yourEvents); // Need to pass on so it can return to the proper fragment

            Navigation.findNavController(view)
                    .navigate(R.id.action_browseEventsFragment_to_eventDetailsFragment, args);
        });

        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Following lines set the default behavior of the fragment to the default browsing mode
        searchEventsText.setVisibility(View.VISIBLE);
        filterBar.setVisibility(View.VISIBLE);
        filterButtons.setVisibility(View.VISIBLE);
        adapter.setDetailed(false);

        // Load the source data first, then allow UI controls to refine the in-memory list.
        loadEvents();
        wireSearchAndFilters(view);

        toggleDetailedView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    searchEventsText.setVisibility(View.GONE);
                    filterBar.setVisibility(View.GONE);
                    filterButtons.setVisibility(View.GONE);
                    recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
                    snapHelper = new LinearSnapHelper();
                    snapHelper.attachToRecyclerView(recyclerView);
                    adapter.setDetailed(true);
                } else {
                    searchEventsText.setVisibility(View.VISIBLE);
                    filterBar.setVisibility(View.VISIBLE);
                    filterButtons.setVisibility(View.VISIBLE);
                    recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                    // Detach the snaphelper if we are coming back from a detailed view.
                    if (snapHelper != null) {
                        snapHelper.attachToRecyclerView(null);
                        snapHelper = null;
                    }
                    adapter.setDetailed(false);
                }
            }
        });

        // Return to Main Menu from Toolbar
        view.findViewById(R.id.home_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_mainMenuFragment)
                );

        // Go to Profile View from Toolbar
        view.findViewById(R.id.profile_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_viewProfileFragment)
                );

        // Navigate to Notifications
        view.findViewById(R.id.notifications_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_notificationFragment)
                );

        return view;
    }

    private void showDateTimePicker(EditText editText) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCalendar.setTimeInMillis(selection);

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText("Select Time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH));
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ENGLISH);
                editText.setText(sdf.format(calendar.getTime()));
            });

            timePicker.show(getParentFragmentManager(), "TIME_PICKER");
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void wireSearchAndFilters(View view) {
        // Search and structured filters both feed the same applyFilters method so ticket #98
        // ("combine search with filters") is just the shared code path, not a separate screen.
        view.findViewById(R.id.apply_filters_button).setOnClickListener(v -> applyFilters(true));
        view.findViewById(R.id.clear_filters_button).setOnClickListener(v -> clearFilters());
    }

    /**
     * A method that is used to fetch all of the events from the EventRepository so that they can
     * be displayed on screen.
     */
    private void loadEvents() {
        EventRepository repository = new EventRepository();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : null;

        repository.getAllEvents(new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                // Replace the source list atomically, then re-run the current filter state against
                // the fresh data so browse results stay in sync with Firestore.
                allEvents.clear();
                if (yourEvents && currentUserId != null) {
                    for (Event event : events) {
                        if (event.getOrganizerId().equals(currentUserId) ||
                            event.getCoOrganizerIds().contains(currentUserId) ||
                            event.getInvitedUserIds().contains(currentUserId)) {
                            allEvents.add(event);
                        }
                    }
                } else {
                    allEvents.addAll(events);
                }
                applyFilters(false);
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Failed to load events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilters(boolean showValidationErrors) {
        // Parse each optional filter independently. A blank field means that constraint is inactive.
        Long availableAtMillis = parseAvailableAtMillis(showValidationErrors);
        if (showValidationErrors && filterHasValue(filterAvailableAtText) && availableAtMillis == null) {
            return;
        }

        Integer minimumCapacity = parseMinimumCapacity(showValidationErrors);
        if (showValidationErrors && filterHasValue(filterCapacityText) && minimumCapacity == null) {
            return;
        }

        // One service call applies keyword search, availability, and capacity together.
        List<Event> filteredEvents = EventFilterService.filterEvents(
                allEvents,
                searchEventsText.getText().toString(),
                availableAtMillis,
                minimumCapacity
        );
        updateVisibleEvents(filteredEvents);
    }

    private void clearFilters() {
        // Reset both the UI state and the rendered list so "clear" truly restores the full browse view.
        searchEventsText.setText("");
        filterAvailableAtText.setText("");
        filterCapacityText.setText("");
        updateVisibleEvents(allEvents);
    }

    private void updateVisibleEvents(List<Event> filteredEvents) {
        // Private events stay visible in the browse list. Access is enforced on click instead.
        visibleEvents.clear();
        visibleEvents.addAll(filteredEvents);
        adapter.notifyDataSetChanged();
        // Show a dedicated empty state instead of leaving the user with a blank RecyclerView.
        emptyResultsText.setVisibility(visibleEvents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private Long parseAvailableAtMillis(boolean showValidationErrors) {
        String availableAtText = filterAvailableAtText.getText().toString().trim();
        if (availableAtText.isEmpty()) {
            return null;
        }

        // The field represents a single point in time. An event matches if its registration window
        // contains that timestamp.
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ENGLISH);
        dateFormat.setLenient(false);
        try {
            return dateFormat.parse(availableAtText).getTime();
        } catch (ParseException | NullPointerException e) {
            if (showValidationErrors && getContext() != null) {
                Toast.makeText(
                        getContext(),
                        "Please enter a valid date and time",
                        Toast.LENGTH_SHORT
                ).show();
            }
            return null;
        }
    }

    private Integer parseMinimumCapacity(boolean showValidationErrors) {
        String capacityText = filterCapacityText.getText().toString().trim();
        if (capacityText.isEmpty()) {
            return null;
        }

        try {
            // The browse UI uses "minimum capacity" semantics, not exact-capacity matching.
            int minimumCapacity = Integer.parseInt(capacityText);
            if (minimumCapacity <= 0) {
                throw new NumberFormatException("Capacity must be positive");
            }
            return minimumCapacity;
        } catch (NumberFormatException e) {
            if (showValidationErrors && getContext() != null) {
                Toast.makeText(
                        getContext(),
                        "Enter a positive whole number for capacity",
                        Toast.LENGTH_SHORT
                ).show();
            }
            return null;
        }
    }

    private boolean filterHasValue(EditText input) {
        // Shared helper used to distinguish "blank optional field" from "user entered invalid text".
        return !TextUtils.isEmpty(input.getText().toString().trim());
    }
}