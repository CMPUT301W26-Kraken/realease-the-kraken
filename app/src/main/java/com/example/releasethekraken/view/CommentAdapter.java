package com.example.releasethekraken.view;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.releasethekraken.databinding.ItemCommentBinding;
import com.example.releasethekraken.model.Comment;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private final List<Comment> comments;
    private final OnCommentClickListener listener;

    public interface OnCommentClickListener {
        void onCommentClick(Comment comment);
    }

    public CommentAdapter(List<Comment> items, OnCommentClickListener listener) {
        comments = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ItemCommentBinding binding = ItemCommentBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentAdapter.ViewHolder holder, int position) {

        Comment comment = comments.get(position);

        holder.binding.textCommentUser.setText(comment.getAuthorName());
        holder.binding.textCommentContent.setText(comment.getContent());
        holder.binding.textCommentDate.setText(
                DateFormat.format("yyyy-MM-dd HH:mm", comment.getPostDateMillis()).toString()
        );

        if (comment.isOrganizer()) {
            holder.binding.textOrganizerLabel.setText("ORGANIZER");
            holder.binding.textOrganizerLabel.setVisibility(View.VISIBLE);
        } else if (comment.isCoOrganizer()) {
            holder.binding.textOrganizerLabel.setText("CO-ORGANIZER");
            holder.binding.textOrganizerLabel.setVisibility(View.VISIBLE);
        } else {
            holder.binding.textOrganizerLabel.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onCommentClick(comment));
    }

    @Override
    public int getItemCount() { return comments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCommentBinding binding;

        ViewHolder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}