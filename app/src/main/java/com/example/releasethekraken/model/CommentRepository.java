package com.example.releasethekraken.model;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * repository responsible for saving and retrieving comments from Firestore
 * this class belongs to the model layer and contains no UI logic
 */
public class CommentRepository {

    private final FirebaseFirestore db;

    /**
     * creates a CommentRepository using the default Firestore instance
     */
    public CommentRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * creates a CommentRepository with a specific Firestore instance
     *
     * @param db the Firestore database instance to use
     */
    public CommentRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * callback interface for operations that only need success or failure
     */
    public interface CompletionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    /**
     * callback interface for operations that return a list of comments
     */
    public interface CommentsCallback {
        void onSuccess(List<Comment> comments);
        void onError(Exception e);
    }

    /**
     * adds a comment to the event's comments subcollection
     *
     * @param comment the comment to store
     * @param callback callback for success or failure
     */
    public void addComment(Comment comment, CompletionCallback callback) {
        if (comment == null || comment.getEventId() == null) {
            callback.onError(new IllegalArgumentException("invalid comment"));
            return;
        }

        db.collection("events")
                .document(comment.getEventId())
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    comment.setCommentId(documentReference.getId());
                    // Update the document with its ID
                    documentReference.update("commentId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * fetches all comments for a given event ordered by post date
     *
     * @param eventId the id of the event whose comments are requested
     * @param callback callback returning the list of comments
     */
    public void getCommentsForEvent(String eventId, CommentsCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("postDateMillis")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> comments = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Comment comment = document.toObject(Comment.class);
                        comment.setCommentId(document.getId());
                        comments.add(comment);
                    }
                    callback.onSuccess(comments);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * deletes a comment from Firestore
     *
     * @param eventId the id of the event the comment belongs to
     * @param commentId the id of the comment to delete
     * @param callback callback for success or failure
     */
    public void deleteComment(String eventId, String commentId, CompletionCallback callback) {
        if (eventId == null || commentId == null) {
            callback.onError(new IllegalArgumentException("invalid eventId or commentId"));
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("comments")
                .document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}