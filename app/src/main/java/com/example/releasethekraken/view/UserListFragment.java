package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.NotificationService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.Notification;
import com.example.releasethekraken.model.NotificationRepository;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment that displays a list of users, such as event participants or attendees.
 * Retrieves user data from the repository and presents it using a RecyclerView.
 *
 */

public class UserListFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ARG_LIST_MODE = "listMode";

    private static final String MODE_WAITING = "waiting";
    private static final String MODE_INVITED = "invited";
    private static final String MODE_CANCELLED = "cancelled";
    private static final String MODE_FINAL = "final";

    private int mColumnCount = 1;
    private boolean adminView;
    private String eventId;
    private UserRole userRole;
    private String listMode = MODE_WAITING;

    private RecyclerView recyclerView;
    private EditText searchUsersText;

    private WaitingListRepository waitingListRepository;
    private EventRepository eventRepository;
    private ProfileRepository profileRepository;
    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    private final List<UserListItem> allItems = new ArrayList<>();
    private final List<UserListItem> visibleItems = new ArrayList<>();
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
        notificationRepository = new NotificationRepository();
        notificationService = new NotificationService(notificationRepository);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            adminView = getArguments().getBoolean("adminView", false);
            eventId = getArguments().getString("eventId");
            userRole = (UserRole) getArguments().getSerializable("userRole");

            String mode = getArguments().getString(ARG_LIST_MODE);
            listMode = mode != null ? mode : MODE_WAITING;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        ImageButton returnButton = view.findViewById(R.id.return_to_details_button);
        recyclerView = view.findViewById(R.id.users_recycler_view);
        searchUsersText = view.findViewById(R.id.search_users_text);
        ImageButton searchUsersButton = view.findViewById(R.id.search_users_button);

        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), mColumnCount));
        }

        adapter = new ProfileListAdapter(visibleItems, adminView,
                (item, position) -> {
                    if (adminView) {
                        onDeleteClicked(item.profile);
                    } else if (MODE_WAITING.equals(listMode) && userRole == UserRole.ORGANIZER) {
                        showCoOrganizerInviteDialog(item.profile);
                    }
                },
                (item, position) -> {
                    if (adminView) {
                        onRevokeOrganizerClicked(item.profile);
                    }
                });
        recyclerView.setAdapter(adapter);

        TextView welcomeText = view.findViewById(R.id.welcome_text);
        if (adminView) {
            welcomeText.setText(getString(R.string.admin_user_list_welcome));
            returnButton.setVisibility(View.GONE);
            loadAllProfiles();
        } else {
            returnButton.setVisibility(View.VISIBLE);
            if (MODE_INVITED.equals(listMode)) {
                welcomeText.setText("Invited Entrants");
                loadInvitedEntrants();
            } else if (MODE_CANCELLED.equals(listMode)) {
                welcomeText.setText("Cancelled / Declined Entrants");
                loadCancelledEntrants();
            } else if (MODE_FINAL.equals(listMode)) {
                welcomeText.setText("Final Attendees");
                loadFinalAttendees();
            } else {
                welcomeText.setText(getString(R.string.waiting_list_welcome));
                loadWaitingList();
            }
        }

        searchUsersButton.setOnClickListener(v -> applyFilter(searchUsersText.getText().toString()));

        view.findViewById(R.id.home_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_mainMenuFragment)
                );

        view.findViewById(R.id.profile_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_viewProfileFragment)
                );

        view.findViewById(R.id.notifications_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_notificationFragment)
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

                                    removeProfileFromLists(profile.getUid());
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

    /**
     * Shows a confirmation dialog and revokes organizer role for the given profile,
     * resetting it to ENTRANT in Firestore.
     */
    private void onRevokeOrganizerClicked(Profile profile) {
        String displayName = (profile.getName() != null && !profile.getName().isEmpty())
                ? profile.getName() : profile.getUid();

        UserRole currentRole = profile.getUserRole();
        if (currentRole != UserRole.ORGANIZER) {
            Toast.makeText(requireContext(), displayName + " is not an organizer.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Revoke Organizer")
                .setMessage("Remove organizer privileges from " + displayName + "? They will become an Entrant.")
                .setPositiveButton("Revoke", (dialog, which) -> {
                    profileRepository.updateUserRole(profile.getUid(), UserRole.ENTRANT,
                            new ProfileRepository.ProfileRepositoryCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (!isAdded()) return;
                                    profile.setRole(UserRole.ENTRANT.name());
                                    // Refresh the subtitle in the list to reflect the new role
                                    updateSubtitleInLists(profile.getUid(), "Role: Entrant");
                                    Toast.makeText(requireContext(),
                                            displayName + "'s organizer role revoked.",
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Exception exception) {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(),
                                            "Failed to revoke role: " + exception.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void removeProfileFromLists(String uid) {
        for (int i = allItems.size() - 1; i >= 0; i--) {
            if (uid.equals(allItems.get(i).profile.getUid())) {
                allItems.remove(i);
            }
        }

        for (int i = visibleItems.size() - 1; i >= 0; i--) {
            if (uid.equals(visibleItems.get(i).profile.getUid())) {
                visibleItems.remove(i);
            }
        }

        adapter.notifyDataSetChanged();
    }

    /**
     * Updates the subtitle text for a specific user in both allItems and visibleItems.
     */
    private void updateSubtitleInLists(String uid, String newSubtitle) {
        for (UserListItem item : allItems) {
            if (uid.equals(item.profile.getUid())) {
                item.subtitle = newSubtitle;
            }
        }
        for (UserListItem item : visibleItems) {
            if (uid.equals(item.profile.getUid())) {
                item.subtitle = newSubtitle;
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadAllProfiles() {
        profileRepository.getAllProfiles(new ProfileRepository.ProfileRepositoryCallback<List<Profile>>() {
            @Override
            public void onSuccess(List<Profile> result) {
                if (!isAdded()) return;

                List<UserListItem> items = new ArrayList<>();
                for (Profile profile : result) {
                    // Show role as subtitle so admin can see who is an organizer
                    String roleLabel = "Role: " + profile.getUserRole().getLabel();
                    items.add(new UserListItem(profile, roleLabel));
                }
                setItems(items);
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
        String name = profile.getName() == null ? "" : profile.getName();
        new AlertDialog.Builder(requireContext())
                .setTitle("Invite Co-Organizer")
                .setMessage("Do you want to invite " + name + " to be a co-organizer for this event?")
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
                if (!isAdded()) return;

                List<UserListItem> items = new ArrayList<>();
                if (entrants.isEmpty()) {
                    setItems(items);
                    return;
                }

                fetchWaitingListProfilesSequentially(entrants, 0, items);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load waiting list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchWaitingListProfilesSequentially(List<String> entrantIds, int index, List<UserListItem> items) {
        if (!isAdded()) return;

        if (index >= entrantIds.size()) {
            setItems(items);
            return;
        }

        String entrantId = entrantIds.get(index);
        profileRepository.getProfileById(entrantId, new ProfileRepository.ProfileRepositoryCallback<Profile>() {
            @Override
            public void onSuccess(Profile result) {
                items.add(new UserListItem(result, ""));
                fetchWaitingListProfilesSequentially(entrantIds, index + 1, items);
            }

            @Override
            public void onFailure(Exception exception) {
                Profile placeholder = new Profile(entrantId, "", "", null);
                placeholder.setUid(entrantId);
                items.add(new UserListItem(placeholder, ""));
                fetchWaitingListProfilesSequentially(entrantIds, index + 1, items);
            }
        });
    }

    private void loadInvitedEntrants() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        notificationRepository.getInvitedEntrantsForEvent(eventId, new NotificationRepository.NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                if (!isAdded()) return;
                loadProfilesFromNotifications(notifications);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load invited entrants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCancelledEntrants() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        notificationRepository.getCancelledEntrantsForEvent(eventId, new NotificationRepository.NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                if (!isAdded()) return;
                loadProfilesFromNotifications(notifications);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load cancelled / declined entrants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFinalAttendees() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        notificationRepository.getFinalAcceptedEntrantsForEvent(eventId, new NotificationRepository.EntrantIdsCallback() {
            @Override
            public void onSuccess(List<String> entrantIds) {
                if (!isAdded()) return;

                List<UserListItem> items = new ArrayList<>();
                if (entrantIds.isEmpty()) {
                    setItems(items);
                    return;
                }

                fetchFinalAttendeeProfilesSequentially(entrantIds, 0, items);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load final attendees", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchFinalAttendeeProfilesSequentially(List<String> entrantIds, int index, List<UserListItem> items) {
        if (!isAdded()) return;

        if (index >= entrantIds.size()) {
            setItems(items);
            return;
        }

        String entrantId = entrantIds.get(index);
        profileRepository.getProfileById(entrantId, new ProfileRepository.ProfileRepositoryCallback<Profile>() {
            @Override
            public void onSuccess(Profile result) {
                items.add(new UserListItem(result, "Attendance Status: Confirmed"));
                fetchFinalAttendeeProfilesSequentially(entrantIds, index + 1, items);
            }

            @Override
            public void onFailure(Exception exception) {
                Profile placeholder = new Profile(entrantId, "", "", null);
                placeholder.setUid(entrantId);
                items.add(new UserListItem(placeholder, "Attendance Status: Confirmed"));
                fetchFinalAttendeeProfilesSequentially(entrantIds, index + 1, items);
            }
        });
    }

    private void loadProfilesFromNotifications(List<Notification> notifications) {
        Map<String, Notification> latestByEntrant = new LinkedHashMap<>();

        for (Notification notification : notifications) {
            String entrantId = notification.getEntrantId();
            if (entrantId == null || entrantId.trim().isEmpty()) {
                continue;
            }

            if (!latestByEntrant.containsKey(entrantId)) {
                latestByEntrant.put(entrantId, notification);
            }
        }

        List<Notification> deduplicated = new ArrayList<>(latestByEntrant.values());
        if (deduplicated.isEmpty()) {
            setItems(new ArrayList<>());
            return;
        }

        fetchNotificationProfilesSequentially(deduplicated, 0, new ArrayList<>());
    }

    private void fetchNotificationProfilesSequentially(List<Notification> notifications,
                                                       int index,
                                                       List<UserListItem> items) {
        if (!isAdded()) return;

        if (index >= notifications.size()) {
            setItems(items);
            return;
        }

        Notification notification = notifications.get(index);
        String entrantId = notification.getEntrantId();
        String statusLabel = buildStatusLabel(notification);

        profileRepository.getProfileById(entrantId, new ProfileRepository.ProfileRepositoryCallback<Profile>() {
            @Override
            public void onSuccess(Profile result) {
                items.add(new UserListItem(result, statusLabel));
                fetchNotificationProfilesSequentially(notifications, index + 1, items);
            }

            @Override
            public void onFailure(Exception exception) {
                Profile placeholder = new Profile(entrantId, "", "", null);
                placeholder.setUid(entrantId);
                items.add(new UserListItem(placeholder, statusLabel));
                fetchNotificationProfilesSequentially(notifications, index + 1, items);
            }
        });
    }

    private String buildStatusLabel(Notification notification) {
        String rawStatus = notification.getResponseStatus();
        String normalizedStatus;

        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            normalizedStatus = "Pending";
        } else if ("accepted".equalsIgnoreCase(rawStatus)) {
            normalizedStatus = "Accepted";
        } else if ("declined".equalsIgnoreCase(rawStatus)) {
            normalizedStatus = "Declined";
        } else if ("cancelled".equalsIgnoreCase(rawStatus)) {
            normalizedStatus = "Cancelled";
        } else {
            normalizedStatus = rawStatus.substring(0, 1).toUpperCase() + rawStatus.substring(1).toLowerCase();
        }

        return "Invitation Status: " + normalizedStatus;
    }

    private void setItems(List<UserListItem> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter(searchUsersText != null ? searchUsersText.getText().toString() : "");
    }

    private void applyFilter(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();

        visibleItems.clear();
        if (normalizedQuery.isEmpty()) {
            visibleItems.addAll(allItems);
        } else {
            for (UserListItem item : allItems) {
                String name = item.profile.getName() == null ? "" : item.profile.getName().toLowerCase();
                String uid = item.profile.getUid() == null ? "" : item.profile.getUid().toLowerCase();
                String subtitle = item.subtitle == null ? "" : item.subtitle.toLowerCase();

                if (name.contains(normalizedQuery)
                        || uid.contains(normalizedQuery)
                        || subtitle.contains(normalizedQuery)) {
                    visibleItems.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    interface OnProfileClickListener {
        void onProfileClick(UserListItem item, int position);
    }

    static class UserListItem {
        final Profile profile;
        String subtitle;

        UserListItem(Profile profile, String subtitle) {
            this.profile = profile;
            this.subtitle = subtitle;
        }
    }

    private static class ProfileListAdapter extends RecyclerView.Adapter<ProfileListAdapter.ProfileViewHolder> {

        private final List<UserListItem> items;
        private final boolean showAdminButtons;
        private final OnProfileClickListener deleteListener;
        private final OnProfileClickListener revokeListener;

        ProfileListAdapter(List<UserListItem> items, boolean showAdminButtons,
                           OnProfileClickListener deleteListener,
                           OnProfileClickListener revokeListener) {
            this.items = items;
            this.showAdminButtons = showAdminButtons;
            this.deleteListener = deleteListener;
            this.revokeListener = revokeListener;
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

            LinearLayout textContainer = new LinearLayout(parent.getContext());
            LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textContainer.setLayoutParams(textContainerParams);
            textContainer.setOrientation(LinearLayout.VERTICAL);

            TextView nameView = new TextView(parent.getContext());
            nameView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            nameView.setTextSize(16);
            nameView.setPadding(0, 8, 0, 4);

            TextView subtitleView = new TextView(parent.getContext());
            subtitleView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            subtitleView.setTextSize(13);
            subtitleView.setPadding(0, 0, 0, 8);

            // Revoke organizer button (shield/block icon — only shown in admin view)
            ImageButton revokeBtn = new ImageButton(parent.getContext());
            LinearLayout.LayoutParams revokeBtnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            revokeBtn.setLayoutParams(revokeBtnParams);
            revokeBtn.setImageDrawable(
                    parent.getContext().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
            revokeBtn.setBackground(null);
            revokeBtn.setContentDescription("Revoke organizer role");

            // Delete button
            ImageButton deleteBtn = new ImageButton(parent.getContext());
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            deleteBtn.setLayoutParams(btnParams);
            deleteBtn.setImageDrawable(
                    parent.getContext().getDrawable(android.R.drawable.ic_menu_delete));
            deleteBtn.setBackground(null);
            deleteBtn.setContentDescription(parent.getContext().getString(R.string.delete_profile_button_description));

            textContainer.addView(nameView);
            textContainer.addView(subtitleView);
            row.addView(textContainer);
            row.addView(revokeBtn);
            row.addView(deleteBtn);

            return new ProfileViewHolder(row, nameView, subtitleView, revokeBtn, deleteBtn);
        }

        @Override
        public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
            UserListItem item = items.get(position);
            Profile profile = item.profile;

            String name = profile.getName();
            holder.nameView.setText((name != null && !name.isEmpty()) ? name : profile.getUid());

            if (TextUtils.isEmpty(item.subtitle)) {
                holder.subtitleView.setVisibility(View.GONE);
            } else {
                holder.subtitleView.setVisibility(View.VISIBLE);
                holder.subtitleView.setText(item.subtitle);
            }

            if (showAdminButtons) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteButton.setOnClickListener(v ->
                        deleteListener.onProfileClick(item, holder.getAdapterPosition()));

                // Only show revoke button if the user is an organizer
                if (profile.getUserRole() == UserRole.ORGANIZER) {
                    holder.revokeButton.setVisibility(View.VISIBLE);
                    holder.revokeButton.setOnClickListener(v ->
                            revokeListener.onProfileClick(item, holder.getAdapterPosition()));
                } else {
                    holder.revokeButton.setVisibility(View.GONE);
                }

                holder.itemView.setOnClickListener(null);
            } else {
                holder.deleteButton.setVisibility(View.GONE);
                holder.revokeButton.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v ->
                        deleteListener.onProfileClick(item, holder.getAdapterPosition()));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ProfileViewHolder extends RecyclerView.ViewHolder {
            TextView nameView;
            TextView subtitleView;
            ImageButton revokeButton;
            ImageButton deleteButton;

            ProfileViewHolder(@NonNull View itemView,
                              TextView nameView,
                              TextView subtitleView,
                              ImageButton revokeButton,
                              ImageButton deleteButton) {
                super(itemView);
                this.nameView = nameView;
                this.subtitleView = subtitleView;
                this.revokeButton = revokeButton;
                this.deleteButton = deleteButton;
            }
        }
    }
}
