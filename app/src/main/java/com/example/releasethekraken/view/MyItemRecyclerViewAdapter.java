package com.example.releasethekraken.view;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.example.releasethekraken.databinding.ItemEventBinding;
import com.example.releasethekraken.databinding.ItemEventDetailedBinding;
import com.example.releasethekraken.model.Event;

import java.util.List;

public class MyItemRecyclerViewAdapter
        extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

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
        // TODO: IMPLEMENT EVENT POSTER FETCHING AND SETTING
        holder.detailedBinding.browseEventTitleDetailed.setText(event.getTitle());
        holder.detailedBinding.browseEventDescriptionDetailed.setText(event.getDescription());
        holder.detailedBinding.browseEventRegcloseDetailed.setText(formatMillis(event.getRegistrationEndMillis()));

        holder.detailedBinding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });

    } else {
            // Compact layout binding
            // TODO: IMPLEMENT EVENT POSTER FETCHING AND SETTING
            holder.binding.itemNumber.setText(event.getTitle());

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
}