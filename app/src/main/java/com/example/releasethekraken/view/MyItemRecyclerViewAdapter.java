package com.example.releasethekraken.view;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.ItemEventBinding;
import com.example.releasethekraken.databinding.ItemEventDetailedBinding;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;
import java.util.Locale;

public class MyItemRecyclerViewAdapter
        extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

    private static final long MAX_POSTER_BYTES = 5L * 1024L * 1024L;
    public static final String ARG_EVENT_ID = "eventId";
    private final List<Event> mValues;
    private final OnEventClickListener listener;
    private boolean isDetailed = false;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public MyItemRecyclerViewAdapter(List<Event> items, OnEventClickListener listener) {
        mValues = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return isDetailed ? 1 : 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 1) {
            ItemEventDetailedBinding detailedBinding =
                    ItemEventDetailedBinding.inflate(inflater, parent, false);
            return new ViewHolder(detailedBinding);
        } else {
            ItemEventBinding binding =
                    ItemEventBinding.inflate(inflater, parent, false);
            return new ViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Event event = mValues.get(position);
        long now = System.currentTimeMillis();
        boolean isOpen = now >= event.getRegistrationStartMillis() && now <= event.getRegistrationEndMillis();

        if (holder.isDetailed) {
            loadPoster(event.getPosterUrl(), holder.detailedBinding.browseEventPosterDetailed);
            holder.detailedBinding.browseEventTitleDetailed.setText(event.getTitle());
            holder.detailedBinding.browseEventDescriptionDetailed.setText(event.getDescription());
            
            String regPeriod = formatMillis(event.getRegistrationStartMillis()) + " - " + formatMillis(event.getRegistrationEndMillis());
            holder.detailedBinding.browseEventRegPeriodDetailed.setText(regPeriod);
            
            holder.detailedBinding.eventOpenBadgeDetailed.setVisibility(isOpen ? View.VISIBLE : View.GONE);

            // Private event logic
            if (event.isPrivate()) {
                holder.detailedBinding.eventPrivateBadgeDetailed.setVisibility(View.VISIBLE);
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                String currentUserId = currentUser != null ? currentUser.getUid() : "";
                
                boolean isOrganizer = event.getOrganizerId().equals(currentUserId);
                boolean isCoOrganizer = event.getCoOrganizerIds().contains(currentUserId);
                boolean isInvited = event.getInvitedUserIds().contains(currentUserId);

                if (isOrganizer || isCoOrganizer || isInvited) {
                    // Invited/Organizer view
                    holder.detailedBinding.browseEventActionButtonDetailed.setText("View Event");
                    holder.detailedBinding.browseEventActionButtonDetailed.setEnabled(true);
                    holder.detailedBinding.browseEventActionButtonDetailed.setBackgroundResource(R.drawable.bg_capsule);
                    holder.detailedBinding.browseEventActionButtonDetailed.getBackground().setTint(holder.itemView.getContext().getColor(R.color.dark_blue_text));
                    holder.detailedBinding.browseEventActionButtonDetailed.setTextColor(Color.WHITE);
                } else {
                    // Not invited view
                    holder.detailedBinding.browseEventActionButtonDetailed.setText("Invite Only");
                    holder.detailedBinding.browseEventActionButtonDetailed.setEnabled(false);
                    holder.detailedBinding.browseEventActionButtonDetailed.setBackgroundResource(R.drawable.bg_capsule);
                    holder.detailedBinding.browseEventActionButtonDetailed.getBackground().setTint(Color.parseColor("#F1F5F9"));
                    holder.detailedBinding.browseEventActionButtonDetailed.setTextColor(Color.parseColor("#475569"));
                }
            } else {
                holder.detailedBinding.eventPrivateBadgeDetailed.setVisibility(View.GONE);
                holder.detailedBinding.browseEventActionButtonDetailed.setText("View Event");
                holder.detailedBinding.browseEventActionButtonDetailed.setEnabled(true);
                holder.detailedBinding.browseEventActionButtonDetailed.getBackground().setTint(holder.itemView.getContext().getColor(R.color.dark_blue_text));
                holder.detailedBinding.browseEventActionButtonDetailed.setTextColor(Color.WHITE);
            }

            // Fetch waitlist count
            new WaitingListRepository().getAllEntrants(event.getEventId(), new WaitingListRepository.EntrantsCallback() {
                @Override
                public void onResult(List<String> entrants) {
                    int currentPos = holder.getBindingAdapterPosition();
                    if (currentPos == RecyclerView.NO_POSITION) return;
                    
                    if (holder.getBindingAdapterPosition() == position) {
                        holder.detailedBinding.browseEventWaitlistCountDetailed.setText(
                                String.format(Locale.getDefault(), "%d people", entrants.size()));
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Silently fail for UI count
                }
            });

            holder.detailedBinding.browseViewCommentsButtonDetailed.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(ARG_EVENT_ID, event.getEventId());
                args.putSerializable("userRole", UserRole.ENTRANT);
                Navigation.findNavController(v).navigate(R.id.action_browseEventsFragment_to_commentsFragment, args);
            });

            holder.detailedBinding.browseEventActionButtonDetailed.setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });

            holder.detailedBinding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });

        } else {
            loadPoster(event.getPosterUrl(), holder.binding.browseEventPoster);
            holder.binding.itemNumber.setText(event.getTitle());
            
            String dateText = formatShortDate(event.getRegistrationEndMillis());
            holder.binding.eventBrowseRegEnd.setText(dateText);
            
            holder.binding.eventOpenBadge.setVisibility(isOpen ? View.VISIBLE : View.GONE);
            holder.binding.eventPrivateBadge.setVisibility(event.isPrivate() ? View.VISIBLE : View.GONE);

            holder.binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void setDetailed(boolean detailed) {
        this.isDetailed = detailed;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemEventBinding binding;
        ItemEventDetailedBinding detailedBinding;
        boolean isDetailed;

        ViewHolder(ItemEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.isDetailed = false;
        }

        ViewHolder(ItemEventDetailedBinding detailedBinding) {
            super(detailedBinding.getRoot());
            this.detailedBinding = detailedBinding;
            this.isDetailed = true;
        }
    }

    private String formatMillis(long millis) {
        return DateFormat.format("MMM d, yyyy • h:mm a", millis).toString();
    }

    private String formatShortDate(long millis) {
        return DateFormat.format("MMM d • h:mm a", millis).toString();
    }

    private void loadPoster(String posterUrl, ImageView posterImageView) {
        if (TextUtils.isEmpty(posterUrl)) {
            posterImageView.setImageResource(R.drawable.krakenlogov1);
            return;
        }

        FirebaseStorage.getInstance()
                .getReferenceFromUrl(posterUrl)
                .getBytes(MAX_POSTER_BYTES)
                .addOnSuccessListener(bytes -> {
                    posterImageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                })
                .addOnFailureListener(e -> {
                    posterImageView.setImageResource(R.drawable.krakenlogov1);
                });
    }
}
