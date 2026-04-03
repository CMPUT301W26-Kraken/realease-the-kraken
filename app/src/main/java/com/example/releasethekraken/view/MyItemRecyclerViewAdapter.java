package com.example.releasethekraken.view;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.releasethekraken.R;
import com.example.releasethekraken.databinding.ItemEventBinding;
import com.example.releasethekraken.databinding.ItemEventDetailedBinding;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

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

    if (holder.isDetailed) {
        // Detailed layout binding
        loadPoster(event.getPosterUrl(), holder.detailedBinding.browseEventPosterDetailed);

        holder.detailedBinding.browseEventTitleDetailed.setText(event.getTitle());
        holder.detailedBinding.browseEventDescriptionDetailed.setText(event.getDescription());
        holder.detailedBinding.browseEventRegcloseDetailed.setText(formatMillis(event.getRegistrationEndMillis()));

        holder.detailedBinding.browseViewCommentsButtonDetailed.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(ARG_EVENT_ID, event.getEventId());
                // Default to entrant here, could be changed later to actually calculate user relation to this events comments
                args.putSerializable("userRole", UserRole.ENTRANT);
                Navigation.findNavController(v).navigate(R.id.action_browseEventsFragment_to_commentsFragment, args);
        });

        holder.detailedBinding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });

    } else {
            // Compact layout binding
            loadPoster(event.getPosterUrl(), holder.binding.browseEventPoster);

            holder.binding.itemNumber.setText(event.getTitle());
            String registrationEnd = "Registration Ends: " + formatMillis(event.getRegistrationEndMillis());
            holder.binding.eventBrowseRegEnd.setText(registrationEnd);

            holder.binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    // Sets whether or not we are in the detailed view of the event
    public void setDetailed(boolean detailed) {
        this.isDetailed = detailed;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ItemEventBinding binding;
        ItemEventDetailedBinding detailedBinding;
        boolean isDetailed;

        // Constructor for compact view
        ViewHolder(ItemEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.isDetailed = false;
        }

        // Constructor for detailed view
        ViewHolder(ItemEventDetailedBinding detailedBinding) {
            super(detailedBinding.getRoot());
            this.detailedBinding = detailedBinding;
            this.isDetailed = true;
        }
    }

    // Borrowed from event details to properly format the end registration
    private String formatMillis(long millis) {
        return DateFormat.format("yyyy-MM-dd HH:mm", millis).toString();
    }

    // Borrowed from event details for loading the image with some minor modificaitons to support the two view types
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