package com.example.releasethekraken.view;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.releasethekraken.databinding.ItemNotificationBinding;
import com.example.releasethekraken.model.Notification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface NotificationActionListener {
        void onAcceptInvitation(Notification notification);
        void onDeclineInvitation(Notification notification);
    }

    private final List<Notification> notifications;
    private final NotificationActionListener actionListener;

    public NotificationAdapter(List<Notification> notifications, NotificationActionListener actionListener) {
        this.notifications = notifications;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.binding.textNotificationType.setText(notification.getType());
        holder.binding.textNotificationMessage.setText(notification.getMessage());
        holder.binding.textNotificationEventId.setText("Event: " + notification.getEventId());
        holder.binding.textNotificationTime.setText(
                DateFormat.format("yyyy-MM-dd HH:mm", notification.getSentAtMillis()).toString()
        );

        String responseStatus = notification.getResponseStatus();
        if (responseStatus == null || responseStatus.trim().isEmpty()) {
            holder.binding.textInvitationStatus.setVisibility(View.GONE);
        } else {
            holder.binding.textInvitationStatus.setVisibility(View.VISIBLE);
            holder.binding.textInvitationStatus.setText("Status: " + responseStatus);
        }

        if (notification.canAcceptInvitation()) {
            holder.binding.buttonAcceptInvitation.setVisibility(View.VISIBLE);
            holder.binding.buttonDeclineInvitation.setVisibility(View.VISIBLE);

            holder.binding.buttonAcceptInvitation.setEnabled(true);
            holder.binding.buttonDeclineInvitation.setEnabled(true);

            holder.binding.buttonAcceptInvitation.setOnClickListener(v -> {
                holder.binding.buttonAcceptInvitation.setEnabled(false);
                holder.binding.buttonDeclineInvitation.setEnabled(false);
                if (actionListener != null) {
                    actionListener.onAcceptInvitation(notification);
                }
            });

            holder.binding.buttonDeclineInvitation.setOnClickListener(v -> {
                holder.binding.buttonAcceptInvitation.setEnabled(false);
                holder.binding.buttonDeclineInvitation.setEnabled(false);
                if (actionListener != null) {
                    actionListener.onDeclineInvitation(notification);
                }
            });
        } else {
            holder.binding.buttonAcceptInvitation.setVisibility(View.GONE);
            holder.binding.buttonDeclineInvitation.setVisibility(View.GONE);
            holder.binding.buttonAcceptInvitation.setOnClickListener(null);
            holder.binding.buttonDeclineInvitation.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemNotificationBinding binding;

        ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}