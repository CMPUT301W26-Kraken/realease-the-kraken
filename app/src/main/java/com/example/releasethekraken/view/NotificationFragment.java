package com.example.releasethekraken.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.SessionManager;
import com.example.releasethekraken.databinding.FragmentNotificationBinding;
import com.example.releasethekraken.model.Notification;
import com.example.releasethekraken.model.NotificationRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the logged-in entrant's notifications and allows invitation responses.
 */
public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;
    private NotificationRepository notificationRepository;
    private final List<Notification> notifications = new ArrayList<>();
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationRepository = new NotificationRepository();

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }

        binding.homeToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_global_mainMenuFragment)
        );

        binding.profileToolbarButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_global_viewProfileFragment)
        );

        adapter = new NotificationAdapter(notifications, new NotificationAdapter.NotificationActionListener() {
            @Override
            public void onAcceptInvitation(Notification notification) {
                acceptInvitation(notification);
            }

            @Override
            public void onDeclineInvitation(Notification notification) {
                declineInvitation(notification);
            }
        });

        binding.notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.notificationsRecyclerView.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        String entrantId = new SessionManager(requireContext()).getCurrentUserId();
        if (entrantId == null || entrantId.trim().isEmpty()) {
            binding.emptyNotificationsText.setVisibility(View.VISIBLE);
            binding.emptyNotificationsText.setText("Unable to load notifications for this user.");
            return;
        }

        notificationRepository.getNotificationsForEntrant(entrantId, new NotificationRepository.NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> loadedNotifications) {
                if (!isAdded() || binding == null) {
                    return;
                }

                notifications.clear();
                notifications.addAll(loadedNotifications);
                adapter.notifyDataSetChanged();

                binding.emptyNotificationsText.setVisibility(
                        notifications.isEmpty() ? View.VISIBLE : View.GONE
                );
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "Failed to load notifications.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptInvitation(Notification notification) {
        notificationRepository.acceptInvitation(
                notification.getEntrantId(),
                notification.getEventId(),
                notification.getNotificationId(),
                new NotificationRepository.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), "Invitation accepted.", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), "Could not accept invitation.", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    }
                }
        );
    }

    private void declineInvitation(Notification notification) {
        notificationRepository.declineInvitation(
                notification.getEntrantId(),
                notification.getEventId(),
                notification.getNotificationId(),
                new NotificationRepository.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) {
                            return;
                        }

                        // Only show the "replacement draw triggered" toast for actual lottery wins
                        String type = notification.getType();
                        if ("WIN".equalsIgnoreCase(type) || "SELECTED".equalsIgnoreCase(type)) {
                            Toast.makeText(requireContext(), "Invitation declined. Replacement draw triggered.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Invitation declined.", Toast.LENGTH_SHORT).show();
                        }

                        loadNotifications();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), "Could not decline invitation.", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
