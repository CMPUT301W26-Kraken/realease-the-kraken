package com.example.releasethekraken.view;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.releasethekraken.R;
import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.databinding.FragmentCommentsBinding;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.Profile;
import com.example.releasethekraken.model.UserRole;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.repository.ProfileRepository;

/**
 * The class that supports the fragment that shows all of the comments for an event which is determined
 * as an argument when this fragment is opened.
 */
public class CommentsFragment extends Fragment {

    public static final String ARG_EVENT_ID = "eventId";

    private FragmentCommentsBinding binding;
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

        binding = FragmentCommentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.returnToDetailsButton.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

        binding.createCommentButton.setOnClickListener(v -> showCommentCreateDialog(eventId, v));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Shows a confirmation dialog before permanently deleting the profile.
     *
     * @param eventId current event having a comment added to it
     *                // May need a repository of some sort to be added later
     * @param view current fragment view used for navigation
     */
    private void showCommentCreateDialog(String eventId,
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
}