package com.example.releasethekraken.model;

/**
 * represents a single comment left on an event
 * stores the information of the comment including the event id, its author's id,
 * the content of the comment, and the date the comment was posted.
 * this class belongs to the model layer and contains no UI logic
 */
public class Comment {
    private String eventId;
    private String userId; // Will be whatever we use as the document ID for the users
    private String authorName;
    private String content;
    private long postDateMillis;

    /**
     * empty constructor required for Firestore
     */
    public Comment() {}

    /**
     * Creating a new comment object
     *
     * @param eventId the unique ID for the event
     * @param userId the unique ID for the author
     * @param authorName the display name of the author
     * @param content the actual content of the comment stored as a string
     * @param postDateMillis the time when the comment was posted stored as a long in milliseconds
     */
    public Comment(String eventId, String userId, String authorName, String content, long postDateMillis) {
        this.eventId = eventId;
        this.userId = userId;
        this.authorName = authorName;
        this.content = content;
        this.postDateMillis = postDateMillis;
    }

    public String getEventId() {
        return eventId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getContent() {
        return content;
    }

    public long getPostDateMillis() {
        return postDateMillis;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setPostDateMillis(long postDateMillis) {
        this.postDateMillis = postDateMillis;
    }
}