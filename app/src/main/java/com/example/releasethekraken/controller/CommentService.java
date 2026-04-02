package com.example.releasethekraken.controller;

import com.example.releasethekraken.model.Comment;
import com.example.releasethekraken.model.CommentRepository;

import java.util.List;

/**
 * service responsible for handling business logic related to comments
 * this class validates input before delegating to the repository
 */
public class CommentService {

    private final CommentRepository repository;

    /**
     * creates a CommentService with a given repository
     *
     * @param repository the comment repository
     */
    public CommentService(CommentRepository repository) {
        this.repository = repository;
    }

    /**
     * possible results when attempting to add a comment
     */
    public enum AddCommentResult {
        SUCCESS,
        EMPTY_COMMENT,
        INVALID_INPUT,
        ERROR
    }

    /**
     * callback interface for adding a comment
     */
    public interface AddCommentCallback {
        void onResult(AddCommentResult result);
    }

    /**
     * callback interface for fetching comments
     */
    public interface FetchCommentsCallback {
        void onSuccess(List<Comment> comments);
        void onError(Exception e);
    }

    /**
     * submits a comment after validating input
     *
     * @param eventId the event id
     * @param userId the user id
     * @param authorName the user's display name
     * @param content the comment text
     * @param isOrganizer whether the author is the organizer of the event
     * @param isCoOrganizer whether the author is a co-organizer of the event
     * @param callback callback for result
     */
    public void submitComment(String eventId,
                              String userId,
                              String authorName,
                              String content,
                              boolean isOrganizer,
                              boolean isCoOrganizer,
                              AddCommentCallback callback) {

        if (eventId == null || userId == null || authorName == null || content == null) {
            callback.onResult(AddCommentResult.INVALID_INPUT);
            return;
        }

        String trimmed = content.trim();

        if (trimmed.isEmpty()) {
            callback.onResult(AddCommentResult.EMPTY_COMMENT);
            return;
        }

        long timestamp = System.currentTimeMillis();

        Comment comment = new Comment(
                eventId,
                userId,
                authorName,
                trimmed,
                timestamp,
                isOrganizer,
                isCoOrganizer
        );

        repository.addComment(comment, new CommentRepository.CompletionCallback() {
            @Override
            public void onSuccess() {
                callback.onResult(AddCommentResult.SUCCESS);
            }

            @Override
            public void onError(Exception e) {
                callback.onResult(AddCommentResult.ERROR);
            }
        });
    }

    /**
     * fetches comments for a given event
     *
     * @param eventId the event id
     * @param callback callback returning comments
     */
    public void fetchComments(String eventId, FetchCommentsCallback callback) {
        repository.getCommentsForEvent(eventId, new CommentRepository.CommentsCallback() {
            @Override
            public void onSuccess(List<Comment> comments) {
                callback.onSuccess(comments);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
}