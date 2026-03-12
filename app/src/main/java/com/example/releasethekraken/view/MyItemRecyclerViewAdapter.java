package com.example.releasethekraken.view;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.example.releasethekraken.databinding.ItemEventBinding;
import com.example.releasethekraken.model.Event;

import java.util.List;

public class MyItemRecyclerViewAdapter
        extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

    private final List<Event> mValues;
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public MyItemRecyclerViewAdapter(List<Event> items, OnEventClickListener listener) {
        mValues = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ItemEventBinding binding = ItemEventBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        Event event = mValues.get(position);

        holder.binding.itemNumber.setText(event.getTitle());
        holder.binding.content.setText(event.getDescription());

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ItemEventBinding binding;

        ViewHolder(ItemEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}