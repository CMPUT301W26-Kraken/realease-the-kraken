package com.example.releasethekraken.model;

/**
 * represents a notification sent to an entrant
 * stores information about which entrant received the notification,
 * which event it relates to, the message content, the notification
 * type, and the time the notification was sent.
 * this class belongs to the model layer and contains no UI logic
 */
public class Notification {

    private final String notificationId;
    private final String entrantId;
    private final String eventId;
    private final String message;
    private final String type;
    private final long sentAtMillis;
    private final boolean read;
    private final String responseStatus;

    /**
     * Backward-compatible constructor for older call sites.
     */
    public Notification(String entrantId, String eventId, String message, String type, long sentAtMillis) {
        this(null, entrantId, eventId, message, type, sentAtMillis, false, null);
    }

    public Notification(String notificationId,
                        String entrantId,
                        String eventId,
                        String message,
                        String type,
                        long sentAtMillis,
                        boolean read,
                        String responseStatus) {
        this.notificationId = notificationId;
        this.entrantId = entrantId;
        this.eventId = eventId;
        this.message = message;
        this.type = type;
        this.sentAtMillis = sentAtMillis;
        this.read = read;
        this.responseStatus = responseStatus;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getEntrantId() {
        return entrantId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public long getSentAtMillis() {
        return sentAtMillis;
    }

    public boolean isRead() {
        return read;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public boolean isInvitation() {
        return type != null && (type.equalsIgnoreCase("win") || type.equalsIgnoreCase("invitation"));
    }

    public boolean canAcceptInvitation() {
        return isInvitation()
                && (responseStatus == null
                || responseStatus.trim().isEmpty()
                || !responseStatus.equalsIgnoreCase("accepted"));
    }
}