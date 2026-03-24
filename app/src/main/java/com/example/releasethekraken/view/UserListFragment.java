package com.example.releasethekraken.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.releasethekraken.R;
import com.example.releasethekraken.model.UserRole;

/**
 * UserListFragment is a fragment that displays a list of users. This fragment can either be used
 * by admins by navigating to it from the main menu in order to see all the users or from an event
 * details page as an organizer or admin in order to see users who are in the waiting list for that event.
 *
 * Takes a Boolean argument called adminView that sets visibility and determines if all users are going
 * to be shown, or just those belonging to a specific event's waiting list.
 * Takes a String argument called eventId that corresponds the event's id so that it fetches the proper
 * waiting list.
 * Takes a UserRole argument userRole that determines the role of the user, whether it is an admin
 * viewing the all users list or an organizer viewing their waiting list makes a difference here.
 */
public class UserListFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private boolean adminView;
    private String eventId;
    private UserRole userRole;
    // private final List<Profile> userList = new ArrayList<>();
    // Will need an adapter to view items at some point

    public UserListFragment() {}

    public static UserListFragment newInstance(int columnCount) {
        UserListFragment fragment = new UserListFragment();
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
            adminView = getArguments().getBoolean("adminView", false);
            eventId = getArguments().getString("eventId");
            userRole = (UserRole) getArguments().getSerializable("userRole");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        ImageButton returnButton = view.findViewById(R.id.return_to_details_button);

        // Set the welcome text based on admin view or event waiting list view
        TextView welcomeText = view.findViewById(R.id.welcome_text);
        if (adminView) {
            welcomeText.setText(getString(R.string.admin_user_list_welcome));
            returnButton.setVisibility(View.GONE);
        } else {
            welcomeText.setText(getString(R.string.waiting_list_welcome));
            returnButton.setVisibility(View.VISIBLE);
        }

        // TODO: IMPLEMENT RECYCLER VIEW (OF ALL USERS FOR ADMIN AND ONLY REGISTERED ONES FOR EVENT) AND ASSOCIATED CLICK ACTIONS

        // Return to Main Menu from Toolbar
        view.findViewById(R.id.home_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_userListFragment_to_mainMenuFragment)
                );

        // Go to Profile View from Toolbar
        view.findViewById(R.id.profile_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_userListFragment_to_viewProfileFragment)
                );

        // Navigate to Notifications
        view.findViewById(R.id.notifications_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_userListFragment_to_notificationFragment)
                );

        // Navigate back to event details
        returnButton.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

        return view;
    }
}