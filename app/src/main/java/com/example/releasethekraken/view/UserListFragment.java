package com.example.releasethekraken.view;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.releasethekraken.R;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;

import java.util.ArrayList;
import java.util.List;

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
    private RecyclerView recyclerView;
    private WaitingListRepository waitingListRepository;
    private ProfileRepository profileRepository;
    private final List<String> userList = new ArrayList<>();
    private UserListAdapter adapter;

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
        waitingListRepository = new WaitingListRepository();
        profileRepository = new ProfileRepository(requireContext());

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
        recyclerView = view.findViewById(R.id.users_recycler_view);

        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), mColumnCount));
        }

        adapter = new UserListAdapter(userList);
        recyclerView.setAdapter(adapter);

        // Set the welcome text based on admin view or event waiting list view
        TextView welcomeText = view.findViewById(R.id.welcome_text);
        if (adminView) {
            welcomeText.setText(getString(R.string.admin_user_list_welcome));
            returnButton.setVisibility(View.GONE);
        } else {
            welcomeText.setText(getString(R.string.waiting_list_welcome));
            returnButton.setVisibility(View.VISIBLE);
            loadWaitingList();
        }

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

    private void loadWaitingList() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        waitingListRepository.getAllEntrants(eventId, new WaitingListRepository.EntrantsCallback() {
            @Override
            public void onResult(List<String> entrants) {
                userList.clear();
                adapter.notifyDataSetChanged();

                for (String entrantId : entrants) {
                    profileRepository.getProfileById(entrantId, new ProfileRepository.ProfileRepositoryCallback<Profile>() {
                        @Override
                        public void onSuccess(Profile result) {
                            if (result.getName() != null && !result.getName().isEmpty()) {
                                userList.add(result.getName());
                            } else {
                                userList.add(entrantId);
                            }
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            userList.add(entrantId);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Failed to load waiting list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

        private final List<String> users;

        UserListAdapter(List<String> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            textView.setPadding(32, 24, 32, 24);
            textView.setTextSize(16);
            return new UserViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            holder.textView.setText(users.get(position));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }
}