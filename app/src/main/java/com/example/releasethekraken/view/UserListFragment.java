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
import com.example.releasethekraken.controller.NotificationService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.NotificationRepository;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;

import java.util.ArrayList;
import java.util.List;

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
    private NotificationService notificationService;
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
        notificationService = new NotificationService(new NotificationRepository());

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
                onDeleteClicked(profile);
            } else if (userRole == UserRole.ORGANIZER) {
                showCoOrganizerInviteDialog(profile);
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

    private void onDeleteClicked(Profile profile) {
        String displayName = (profile.getName() != null && !profile.getName().isEmpty())
                ? profile.getName() : profile.getUid();

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.remove_profile_title))
                .setMessage(getString(R.string.remove_profile_message, displayName))
                .setPositiveButton(getString(R.string.remove_profile_confirm), (dialog, which) -> {
                    profileRepository.deleteProfileFromFirestore(profile.getUid(),
                            new ProfileRepository.ProfileRepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (!isAdded()) return;
                                    int index = -1;
                                    for (int i = 0; i < profileList.size(); i++) {
                                        if (profileList.get(i).getUid().equals(profile.getUid())) {
                                            index = i;
                                            break;
                                        }
                                    }
                                    if (index != -1) {
                                        profileList.remove(index);
                                        adapter.notifyItemRemoved(index);
                                        adapter.notifyItemRangeChanged(index, profileList.size());
                                    }
                                    Toast.makeText(requireContext(),
                                            getString(R.string.remove_profile_success, displayName),
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Exception exception) {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(),
                                            getString(R.string.remove_profile_failure, exception.getMessage()),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadAllProfiles() {
        profileRepository.getAllProfiles(new ProfileRepository.ProfileRepositoryCallback<List<Profile>>() {
            @Override
            public void onSuccess(List<Profile> result) {
                if (!isAdded()) return;
                profileList.clear();
                profileList.addAll(result);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception exception) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        getString(R.string.load_profiles_failure), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCoOrganizerInviteDialog(Profile profile) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Invite Co-Organizer")
                .setMessage("Do you want to invite " + profile.getName() + " to be a co-organizer for this event?")
                .setPositiveButton("Invite", (dialog, which) -> inviteCoOrganizer(profile))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void inviteCoOrganizer(Profile profile) {
        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                notificationService.sendCoOrganizerNotification(event, profile.getUid(), new NotificationService.NotificationCallback() {
                    @Override
                    public void onResult(NotificationService.NotificationResult result) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Invitation sent to " + profile.getName(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to send invitation", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load event details", Toast.LENGTH_SHORT).show();
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

    interface OnProfileClickListener {
        void onProfileClick(Profile profile, int position);
    }

    private static class ProfileListAdapter extends RecyclerView.Adapter<ProfileListAdapter.ProfileViewHolder> {

        private final List<Profile> profiles;
        private final boolean showDeleteButton;
        private final OnProfileClickListener clickListener;

        ProfileListAdapter(List<Profile> profiles, boolean showDeleteButton, OnProfileClickListener clickListener) {
            this.profiles = profiles;
            this.showDeleteButton = showDeleteButton;
            this.clickListener = clickListener;
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
            deleteBtn.setContentDescription(parent.getContext().getString(R.string.delete_profile_button_description));

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
                        clickListener.onProfileClick(profile, holder.getAdapterPosition()));
            } else {
                holder.deleteButton.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v ->
                        clickListener.onProfileClick(profile, holder.getAdapterPosition()));
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