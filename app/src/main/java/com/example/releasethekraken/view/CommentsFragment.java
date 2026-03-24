package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.releasethekraken.R;
import com.example.releasethekraken.model.Comment;
import com.example.releasethekraken.model.UserRole;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that supports the fragment that shows all of the comments for an event which is determined
 * as an argument when this fragment is opened.
 */
public class CommentsFragment extends Fragment {

    public static final String ARG_EVENT_ID = "eventId";
    private final List<Comment> comments = new ArrayList<>();
    //private FragmentCommentsBinding binding;
    private CommentAdapter adapter;
    private String eventId;
    private UserRole userRole; // Will be needed for determining if comments can be deleted

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            if (eventId == null) {
                eventId = getArguments().getString("eventId");
            }
            userRole = (UserRole) getArguments().getSerializable("UserType");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_comments, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.comments_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CommentAdapter(comments, comment -> {
            // TODO: ADD LOGIC TO CHECK IF USER IS AN ORGANIZER/ADMIN WHO HAS THE ABILITY TO DELETE COMMENTS
            //showDeleteConfirmationDialog
        });

        recyclerView.setAdapter(adapter);

        loadComments();

        Button createCommentButton = view.findViewById(R.id.create_comment_button);
        createCommentButton.setOnClickListener(v -> showCommentCreateDialog(eventId, v));

        view.findViewById(R.id.return_to_details_button).setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

        return view;
    }

    /**
     * Shows a confirmation dialog before permanently deleting the profile.
     *
     * @param eventId current event having a comment added to it
     * //@param userId current user ID of the user writing the comment
     * @param view current fragment view used for navigation
     */
    private void showCommentCreateDialog(String eventId,
                                              //String userId,
                                              View view) {

        EditText input = new EditText(requireContext());
        input.setHint("Enter Comment Message");
        input.setMinLines(7);
        input.setGravity(Gravity.TOP);

        new AlertDialog.Builder(requireContext())
                .setTitle("Write a Comment")
                .setView(input)
                .setPositiveButton("Post Comment", (dialog, which) -> {

                    String commentText = input.getText().toString().trim();

                    if (!commentText.isEmpty()) {

                        // TODO: ADD FIREBASE COMMENT STORING

                        Toast.makeText(requireContext(),
                                "Comment would be created",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                "Comment cannot be empty",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadComments() {
        // TODO: PULL ACTUAL COMMENTS FROM THE FIREBASE

        Comment test1 = new Comment(eventId, "Test1", "I am organizer making comment", System.currentTimeMillis());
        Comment test2 = new Comment(eventId, "Test2", "I am entrant making comment", System.currentTimeMillis());

        comments.add(test1);
        comments.add(test2);
    }
}