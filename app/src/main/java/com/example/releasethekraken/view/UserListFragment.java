package com.example.releasethekraken.view;

import android.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.releasethekraken.R;
import com.example.releasethekraken.model.EventRepository;
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
    private EventRepository eventRepository;
    private ProfileRepository profileRepository;
    private final List<Profile> profileList = new ArrayList<>();
    private ProfileListAdapter adapter;

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
        eventRepository = new EventRepository();
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

        adapter = new ProfileListAdapter(profileList, adminView, (profile, position) -> {
            if (adminView) {
                onDeleteClicked(profile, position);
            } else if (userRole == UserRole.ORGANIZER) {
                showCoOrganizerDialog(profile);
            }
        });
        recyclerView.setAdapter(adapter);

        TextView welcomeText = view.findViewById(R.id.welcome_text);
        if (adminView) {
            welcomeText.setText(getString(R.string.admin_user_list_welcome));
            returnButton.setVisibility(View.GONE);
            loadAllProfiles();
        } else {
            welcomeText.setText(getString(R.string.waiting_list_welcome));
            returnButton.setVisibility(View.VISIBLE);
            loadWaitingList();
        }

        view.findViewById(R.id.home_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_userListFragment_to_mainMenuFragment)
                );

        view.findViewById(R.id.profile_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_userListFragment_to_viewProfileFragment)
                );

        view.findViewById(R.id.notifications_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_userListFragment_to_notificationFragment)
                );

        returnButton.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        return view;
    }

    /**
     * Called when the admin taps the delete button on a profile row.
     * Shows a confirmation dialog before deleting from Firestore.
     */
    private void onDeleteClicked(Profile profile, int position) {
        String displayName = (profile.getName() != null && !profile.getName().isEmpty())
                ? profile.getName() : profile.getUid();

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Profile")
                .setMessage("Are you sure you want to remove " + displayName + "? This cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    profileRepository.deleteProfileFromFirestore(profile.getUid(),
                            new ProfileRepository.ProfileRepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (!isAdded()) return;
                                    profileList.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    adapter.notifyItemRangeChanged(position, profileList.size());
                                    Toast.makeText(requireContext(),
                                            displayName + " removed.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Exception exception) {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(),
                                            "Failed to remove profile: " + exception.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Fetches all profiles from Firestore and populates the RecyclerView.
     * Used when the fragment is opened in admin view.
     */
    private void loadAllProfiles() {
        profileRepository.getAllProfiles(new ProfileRepository.ProfileRepositoryCallback<List<Profile>>() {
            @Override
            public void onSuccess(List<Profile> result) {
                profileList.clear();
                profileList.addAll(result);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception exception) {
                Toast.makeText(requireContext(), "Failed to load profiles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCoOrganizerDialog(Profile profile) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Assign Co-Organizer")
                .setMessage("Do you want to assign " + profile.getName() + " as a co-organizer for this event? They will be removed from the waiting list.")
                .setPositiveButton("Assign", (dialog, which) -> assignCoOrganizer(profile))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void assignCoOrganizer(Profile profile) {
        eventRepository.addCoOrganizer(eventId, profile.getUid(), new EventRepository.CompletionCallback() {
            @Override
            public void onSuccess() {
                waitingListRepository.removeFromWaitingList(eventId, profile.getUid(), new WaitingListRepository.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), profile.getName() + " is now a co-organizer", Toast.LENGTH_SHORT).show();
                            loadWaitingList();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to remove from waiting list", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to assign co-organizer", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadWaitingList() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        waitingListRepository.getAllEntrants(eventId, new WaitingListRepository.EntrantsCallback() {
            @Override
            public void onResult(List<String> entrants) {
                profileList.clear();
                adapter.notifyDataSetChanged();

                for (String entrantId : entrants) {
                    profileRepository.getProfileById(entrantId, new ProfileRepository.ProfileRepositoryCallback<Profile>() {
                        @Override
                        public void onSuccess(Profile result) {
                            profileList.add(result);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            // Add a placeholder profile with just the ID if fetch fails
                            Profile placeholder = new Profile(entrantId, "", "", null);
                            placeholder.setUid(entrantId);
                            profileList.add(placeholder);
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

    /**
     * Callback interface for delete button clicks in the adapter.
     */
    interface OnDeleteClickListener {
        void onDelete(Profile profile, int position);
    }

    /**
     * RecyclerView adapter for displaying a list of profiles.
     * Shows a delete button on each row when in admin view.
     * Supports both admin delete actions and organizer click actions.
     */
    private static class ProfileListAdapter extends RecyclerView.Adapter<ProfileListAdapter.ProfileViewHolder> {

        private final List<Profile> profiles;
        private final boolean showDeleteButton;
        private final OnDeleteClickListener deleteListener;

        ProfileListAdapter(List<Profile> profiles, boolean showDeleteButton, OnDeleteClickListener deleteListener) {
            this.profiles = profiles;
            this.showDeleteButton = showDeleteButton;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(32, 16, 32, 16);

            TextView nameView = new TextView(parent.getContext());
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameView.setLayoutParams(nameParams);
            nameView.setTextSize(16);
            nameView.setPadding(0, 8, 0, 8);

            ImageButton deleteBtn = new ImageButton(parent.getContext());
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            deleteBtn.setLayoutParams(btnParams);
            deleteBtn.setImageDrawable(
                    parent.getContext().getDrawable(android.R.drawable.ic_menu_delete));
            deleteBtn.setBackground(null);

            row.addView(nameView);
            row.addView(deleteBtn);

            return new ProfileViewHolder(row, nameView, deleteBtn);
        }

        @Override
        public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
            Profile profile = profiles.get(position);
            String name = profile.getName();
            holder.nameView.setText((name != null && !name.isEmpty()) ? name : profile.getUid());

            if (showDeleteButton) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteButton.setOnClickListener(v ->
                        deleteListener.onDelete(profile, holder.getAdapterPosition()));
            } else {
                holder.deleteButton.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v ->
                        deleteListener.onDelete(profile, holder.getAdapterPosition()));
            }
        }

        @Override
        public int getItemCount() {
            return profiles.size();
        }

        static class ProfileViewHolder extends RecyclerView.ViewHolder {
            TextView nameView;
            ImageButton deleteButton;

            ProfileViewHolder(@NonNull View itemView, TextView nameView, ImageButton deleteButton) {
                super(itemView);
                this.nameView = nameView;
                this.deleteButton = deleteButton;
            }
        }
    }
}