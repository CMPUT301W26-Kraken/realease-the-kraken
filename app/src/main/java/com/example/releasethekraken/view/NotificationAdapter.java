package com.example.releasethekraken.view;

import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.ItemNotificationBinding;
import com.example.releasethekraken.model.Notification;

import java.util.List;
import java.util.Locale;

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

        // 1. Set Notification Category (Type)
        String type = notification.getType();
        if (type != null) {
            String formattedType = "Notification";
            if (type.equalsIgnoreCase("CO_ORGANIZER")) {
                formattedType = "Co-organizer Invite";
            } else if (type.equalsIgnoreCase("PRIVATE_INVITE")) {
                formattedType = "Private Invite";
            } else if (type.equalsIgnoreCase("WIN") || type.equalsIgnoreCase("SELECTED")) {
                formattedType = "Event Invitation";
            } else {
                formattedType = type.toLowerCase(Locale.ROOT).replace("_", " ");
                formattedType = Character.toUpperCase(formattedType.charAt(0)) + formattedType.substring(1);
            }
            holder.binding.textNotificationType.setText(formattedType);
        }

        // 2. Set Clean Event Name (Remove unique ID numbers)
        String eventName = notification.getEventTitle();
        if (eventName == null || eventName.trim().isEmpty()) {
            eventName = notification.getEventId();
        }
        
        if (eventName != null) {
            // Remove trailing underscore followed by numbers (common in generated IDs)
            eventName = eventName.replaceAll("_\\d+$", "");
            // Replace remaining underscores with spaces
            eventName = eventName.replace("_", " ");
        }
        holder.binding.textNotificationEventId.setText(eventName);

        // 3. Set Clean Message (Remove redundant event name repetition)
        String message = notification.getMessage();
        if (message != null) {
            // Cut off the message before ": [event name]" or "for event [event name]"
            if (message.contains(":")) {
                message = message.substring(0, message.indexOf(":")).trim();
            } else if (message.toLowerCase().contains("for event")) {
                int index = message.toLowerCase().indexOf("for event");
                message = message.substring(0, index).trim();
            }
        }
        holder.binding.textNotificationMessage.setText(message);

        // 4. Format Time (e.g., Apr 2 · 3:06 PM)
        holder.binding.textNotificationTime.setText(
                DateFormat.format("MMM d · h:mm a", notification.getSentAtMillis()).toString()
        );

        // 5. Status Pill Styling
        String responseStatus = notification.getResponseStatus();
        if (responseStatus == null || responseStatus.trim().isEmpty() || responseStatus.equalsIgnoreCase("pending")) {
            holder.binding.textInvitationStatus.setVisibility(View.GONE);
        } else {
            holder.binding.textInvitationStatus.setVisibility(View.VISIBLE);
            String statusText = responseStatus.substring(0, 1).toUpperCase() + responseStatus.substring(1).toLowerCase();
            holder.binding.textInvitationStatus.setText(statusText);
            
            // Set Pill Color: Green for Accepted, Red for Declined
            if (responseStatus.equalsIgnoreCase("accepted")) {
                 holder.binding.textInvitationStatus.getBackground().setTint(Color.parseColor("#27AE60"));
            } else if (responseStatus.equalsIgnoreCase("declined")) {
                 holder.binding.textInvitationStatus.getBackground().setTint(Color.parseColor("#E74C3C"));
            }
        }

        // 6. Action Buttons logic
        if (notification.canAcceptInvitation()) {
            holder.binding.layoutActionButtons.setVisibility(View.VISIBLE);
            holder.binding.buttonAcceptInvitation.setEnabled(true);
            holder.binding.buttonDeclineInvitation.setEnabled(true);

            holder.binding.buttonAcceptInvitation.setOnClickListener(v -> {
                holder.binding.buttonAcceptInvitation.setEnabled(false);
                holder.binding.buttonDeclineInvitation.setEnabled(false);
                if (actionListener != null) actionListener.onAcceptInvitation(notification);
            });

            holder.binding.buttonDeclineInvitation.setOnClickListener(v -> {
                holder.binding.buttonAcceptInvitation.setEnabled(false);
                holder.binding.buttonDeclineInvitation.setEnabled(false);
                if (actionListener != null) actionListener.onDeclineInvitation(notification);
            });
        } else {
            holder.binding.layoutActionButtons.setVisibility(View.GONE);
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
