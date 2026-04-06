package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.releasethekraken.R;
import com.example.releasethekraken.model.Comment;
import com.example.releasethekraken.model.CommentRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Admin-only fragment that displays all comments across all events in the system.
 * Administrators can delete any comment regardless of event ownership.
 * Reads comments from each event's "comments" subcollection in Firestore.
 * This file was created with the help of generative AI.
 */
public class AdminCommentsFragment extends Fragment {

    private CommentListAdapter adapter;
    private final List<AdminCommentItem> allComments = new ArrayList<>();
    private CommentRepository commentRepository;

    public AdminCommentsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_comments, container, false);

        commentRepository = new CommentRepository();

        RecyclerView recyclerView = view.findViewById(R.id.admin_comments_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommentListAdapter(allComments, this::onDeleteClicked);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.home_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_mainMenuFragment));

        view.findViewById(R.id.profile_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_viewProfileFragment));

        view.findViewById(R.id.notifications_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_notificationFragment));

        fetchAllComments();

        return view;
    }

    /**
     * Fetches all events, then for each event fetches its comments subcollection.
     */
    private void fetchAllComments() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .get()
                .addOnSuccessListener(eventSnapshots -> {
                    if (!isAdded()) return;

                    allComments.clear();
                    int[] pendingEvents = {eventSnapshots.size()};

                    if (pendingEvents[0] == 0) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot eventDoc : eventSnapshots) {
                        String eventId = eventDoc.getId();
                        String eventTitle = eventDoc.getString("title");
                        if (eventTitle == null) eventTitle = eventId;

                        final String finalEventTitle = eventTitle;

                        commentRepository.getCommentsForEvent(eventId, new CommentRepository.CommentsCallback() {
                            @Override
                            public void onSuccess(List<Comment> comments) {
                                if (!isAdded()) return;

                                for (Comment comment : comments) {
                                    allComments.add(new AdminCommentItem(comment, finalEventTitle));
                                }

                                pendingEvents[0]--;
                                if (pendingEvents[0] == 0) {
                                    adapter.notifyDataSetChanged();
                                    if (allComments.isEmpty()) {
                                        Toast.makeText(requireContext(), "No comments found.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                if (!isAdded()) return;
                                pendingEvents[0]--;
                                if (pendingEvents[0] == 0) {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onDeleteClicked(AdminCommentItem item, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Comment")
                .setMessage("Delete comment by " + item.comment.getAuthorName() + "? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    commentRepository.deleteComment(
                            item.comment.getEventId(),
                            item.comment.getCommentId(),
                            new CommentRepository.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    if (!isAdded()) return;
                                    allComments.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    adapter.notifyItemRangeChanged(position, allComments.size());
                                    Toast.makeText(requireContext(), "Comment deleted.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Data model ───────────────────────────────────────────────────────────

    static class AdminCommentItem {
        final Comment comment;
        final String eventTitle;

        AdminCommentItem(Comment comment, String eventTitle) {
            this.comment    = comment;
            this.eventTitle = eventTitle;
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    interface OnDeleteClickListener {
        void onDelete(AdminCommentItem item, int position);
    }

    private static class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.CommentViewHolder> {

        private final List<AdminCommentItem> items;
        private final OnDeleteClickListener deleteListener;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault());

        CommentListAdapter(List<AdminCommentItem> items, OnDeleteClickListener deleteListener) {
            this.items          = items;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(32, 16, 32, 16);

            LinearLayout textContainer = new LinearLayout(parent.getContext());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textContainer.setLayoutParams(textParams);
            textContainer.setOrientation(LinearLayout.VERTICAL);

            TextView authorView = new TextView(parent.getContext());
            authorView.setTextSize(15);
            authorView.setTextColor(0xFF1E293B);
            authorView.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView eventView = new TextView(parent.getContext());
            eventView.setTextSize(12);
            eventView.setTextColor(0xFF64748B);

            TextView contentView = new TextView(parent.getContext());
            contentView.setTextSize(13);
            contentView.setTextColor(0xFF475569);
            contentView.setPadding(0, 4, 0, 4);

            TextView timeView = new TextView(parent.getContext());
            timeView.setTextSize(11);
            timeView.setTextColor(0xFF94A3B8);

            ImageButton deleteBtn = new ImageButton(parent.getContext());
            deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            deleteBtn.setImageDrawable(
                    parent.getContext().getDrawable(android.R.drawable.ic_menu_delete));
            deleteBtn.setBackground(null);
            deleteBtn.setContentDescription("Delete comment");

            textContainer.addView(authorView);
            textContainer.addView(eventView);
            textContainer.addView(contentView);
            textContainer.addView(timeView);
            row.addView(textContainer);
            row.addView(deleteBtn);

            return new CommentViewHolder(row, authorView, eventView, contentView, timeView, deleteBtn);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            AdminCommentItem item = items.get(position);
            Comment comment = item.comment;

            holder.authorView.setText(comment.getAuthorName() != null ? comment.getAuthorName() : "Unknown");
            holder.eventView.setText("Event: " + item.eventTitle);
            holder.contentView.setText(comment.getContent());
            holder.timeView.setText(dateFormat.format(new Date(comment.getPostDateMillis())));

            holder.deleteBtn.setOnClickListener(v ->
                    deleteListener.onDelete(item, holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView authorView, eventView, contentView, timeView;
            ImageButton deleteBtn;

            CommentViewHolder(@NonNull View itemView, TextView authorView, TextView eventView,
                              TextView contentView, TextView timeView, ImageButton deleteBtn) {
                super(itemView);
                this.authorView  = authorView;
                this.eventView   = eventView;
                this.contentView = contentView;
                this.timeView    = timeView;
                this.deleteBtn   = deleteBtn;
            }
        }
    }
}