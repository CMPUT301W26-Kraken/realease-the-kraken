package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.CommentService;
import com.example.releasethekraken.model.Comment;
import com.example.releasethekraken.model.CommentRepository;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.repository.ProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that supports the fragment that shows all of the comments for an event which is determined
 * as an argument when this fragment is opened.
 */
public class CommentsFragment extends Fragment {

    public static final String ARG_EVENT_ID = "eventId";
    private final List<Comment> comments = new ArrayList<>();
    private CommentAdapter adapter;
    private String eventId;
    private UserRole userRole;
    private CommentService commentService;
    private Event currentEvent;
    private boolean isOrganizerOrCoOrganizer = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            if (eventId == null) {
                eventId = getArguments().getString("eventId");
            }
            userRole = (UserRole) getArguments().getSerializable("UserType");
            if (userRole == null) {
                userRole = (UserRole) getArguments().getSerializable("userRole");
            }
        }

        commentService = new CommentService(new CommentRepository());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_comments, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.comments_recycler_view);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new CommentAdapter(comments, new CommentAdapter.OnCommentClickListener() {
                @Override
                public void onCommentClick(Comment comment) {
                    // Optional: show comment details or do nothing
                }

                @Override
                public void onDeleteClick(Comment comment) {
                    showDeleteConfirmationDialog(comment);
                }
            });
            recyclerView.setAdapter(adapter);
        }

        loadEventAndComments();

        Button createCommentButton = view.findViewById(R.id.create_comment_button);
        if (createCommentButton != null) {
            createCommentButton.setOnClickListener(v -> showCommentCreateDialog(eventId));
        }

        View returnButton = view.findViewById(R.id.return_to_details_button);
        if (returnButton != null) {
            returnButton.setOnClickListener(v -> {
                Navigation.findNavController(v).popBackStack();
            });
        }

        return view;
    }

    private void showDeleteConfirmationDialog(Comment comment) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteComment(comment);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteComment(Comment comment) {
        commentService.deleteComment(eventId, comment.getCommentId(), isOrganizerOrCoOrganizer, new CommentService.DeleteCommentCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Comment deleted", Toast.LENGTH_SHORT).show();
                loadComments();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to delete comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEventAndComments() {
        if (TextUtils.isEmpty(eventId)) return;

        new EventRepository().getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                if (!isAdded()) return;
                currentEvent = event;
                checkPermissions();
                loadComments();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                loadComments(); // Still try to load comments
            }
        });
    }

    private void checkPermissions() {
        if (currentEvent == null) return;

        ProfileRepository profileRepository = new ProfileRepository(requireContext());
        Profile profile = profileRepository.getProfile();
        String currentUserId = (profile != null) ? profile.getUid() : "";

        if (TextUtils.isEmpty(currentUserId)) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                currentUserId = firebaseUser.getUid();
            }
        }

        if (!TextUtils.isEmpty(currentUserId)) {
            boolean isOrganizer = currentUserId.equals(currentEvent.getOrganizerId());
            boolean isCoOrganizer = currentEvent.getCoOrganizerIds() != null && currentEvent.getCoOrganizerIds().contains(currentUserId);
            isOrganizerOrCoOrganizer = isOrganizer || isCoOrganizer;
            
            if (adapter != null) {
                adapter.setOrganizerOrCoOrganizer(isOrganizerOrCoOrganizer);
            }
        }
    }

    private void showCommentCreateDialog(String eventId) {
        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(requireContext(), "Error: Missing Event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(requireContext());
        input.setHint("Enter Comment Message");
        input.setMinLines(5);
        input.setGravity(Gravity.TOP);

        new AlertDialog.Builder(requireContext())
                .setTitle("Write a Comment")
                .setView(input)
                .setPositiveButton("Post Comment", (dialog, which) -> {
                    String commentText = input.getText().toString().trim();

                    if (commentText.isEmpty()) {
                        Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ProfileRepository profileRepository = new ProfileRepository(requireContext());
                    Profile profile = profileRepository.getProfile();
                    
                    String userId = (profile != null) ? profile.getUid() : "";
                    String authorName = (profile != null) ? profile.getName() : "";
                    String authorProfileImage = (profile != null) ? profile.getProfileImageUrl() : "";

                    // Fallback to FirebaseAuth if local profile UID is empty
                    if (TextUtils.isEmpty(userId)) {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            userId = firebaseUser.getUid();
                        }
                    }

                    if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(authorName)) {
                        Toast.makeText(requireContext(), "Please set up your name in profile first", Toast.LENGTH_LONG).show();
                        return;
                    }

                    boolean isOrganizer = currentEvent != null && userId.equals(currentEvent.getOrganizerId());
                    boolean isCoOrganizer = currentEvent != null && currentEvent.getCoOrganizerIds().contains(userId);

                    commentService.submitComment(eventId, userId, authorProfileImage, authorName, commentText, isOrganizer, isCoOrganizer, result -> {
                        if (!isAdded()) return;
                        
                        switch (result) {
                            case SUCCESS:
                                Toast.makeText(requireContext(), "Comment posted", Toast.LENGTH_SHORT).show();
                                loadComments();
                                break;
                            case EMPTY_COMMENT:
                                Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                                break;
                            case INVALID_INPUT:
                                Toast.makeText(requireContext(), "Error: Invalid data", Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR:
                                Toast.makeText(requireContext(), "Server error: Failed to post comment", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadComments() {
        if (TextUtils.isEmpty(eventId)) {
            return;
        }
        commentService.fetchComments(eventId, new CommentService.FetchCommentsCallback() {
            @Override
            public void onSuccess(List<Comment> fetchedComments) {
                if (!isAdded()) return;
                comments.clear();
                comments.addAll(fetchedComments);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                View currentView = getView();
                if (currentView != null) {
                    TextView noCommentsText = currentView.findViewById(R.id.no_comments_text);
                    RecyclerView recyclerView = currentView.findViewById(R.id.comments_recycler_view);

                    if (comments.isEmpty()) {
                        if (noCommentsText != null) noCommentsText.setVisibility(View.VISIBLE);
                        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                    } else {
                        if (noCommentsText != null) noCommentsText.setVisibility(View.GONE);
                        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
            }
        });
    }
}