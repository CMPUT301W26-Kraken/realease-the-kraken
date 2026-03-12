package com.example.releasethekraken.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.releasethekraken.MainActivity;
import com.example.releasethekraken.R;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.placeholder.PlaceholderContent;
import com.google.firebase.firestore.auth.User;

public class BrowseEventsFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 2;
    private boolean yourEvents;
    private UserRole userRole = UserRole.ENTRANT;

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
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            yourEvents = getArguments().getBoolean("yourEvents", false);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_browse_events, container, false);

        // Set the welcome text to the proper display based on if we are browsing your events or just normal events
        TextView welcomeText = view.findViewById(R.id.welcome_text);
        if (yourEvents) {
            welcomeText.setText(getString(R.string.your_events_welcome));
        } else {
            welcomeText.setText(getString(R.string.browse_events_welcome));
        }

        // Find RecyclerView inside layout
        RecyclerView recyclerView = view.findViewById(R.id.events_recycler_view);

        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), mColumnCount));
        }

        // TODO: Change the adapter to the events viewing adapter
        recyclerView.setAdapter(
                new MyItemRecyclerViewAdapter(PlaceholderContent.ITEMS, item -> {
                    Bundle args = new Bundle();
                    args.putString("eventId", item.id);

                    // TODO: Implement logic that can determine user type before navigation
                    args.putSerializable("UserType", userRole);

                    args.putBoolean("cameFromYourEvents", yourEvents); // Need to pass on so it can return to the proper fragment

                    Navigation.findNavController(view)
                            .navigate(R.id.action_browseEventsFragment_to_eventDetailsFragment, args);
                })
        );


        Button createEventButton = view.findViewById(R.id.create_event_button);
        // Hide the create events button if we aren't in your Events
        if (!yourEvents) {createEventButton.setVisibility(View.GONE);}

        // Navigate to Create Events
        createEventButton
                .setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putBoolean("editEvent", false);
                    args.putBoolean("cameFromYourEvents", true);

                    Navigation.findNavController(v)
                            .navigate(R.id.action_browseEventsFragment_to_createEventFragment, args);
                });

        // Return to Main Menu from Toolbar
        view.findViewById(R.id.home_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_browseEventsFragment_to_mainMenuFragment)
                );

        // Go to Profile View from Toolbar
        view.findViewById(R.id.profile_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_browseEventsFragment_to_viewProfileFragment)
                );

        // Navigate to Notifications
        view.findViewById(R.id.notifications_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_browseEventsFragment_to_notificationFragment)
                );

        // TODO: REMOVE DUMMY TEST BUTTON + FUNCTION
        // Become an organizer for viewing event details
        view.findViewById(R.id.dummy_organizer_button).setOnClickListener(v -> {
            userRole = UserRole.ORGANIZER;
        });

        // TODO: REMOVE DUMMY TEST BUTTON + FUNCTION
        // Become an entrant for viewing event details
        view.findViewById(R.id.dummy_entrant_button).setOnClickListener(v -> {
            userRole = UserRole.ENTRANT;
        });

        return view;
    }
}