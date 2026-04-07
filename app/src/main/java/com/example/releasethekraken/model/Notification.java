package com.example.releasethekraken.model;

/**
 * Represents a notification sent to an entrant.
 * Stores information about which entrant received the notification,
 * which event it relates to, the message content, the notification
 * type, and the time the notification was sent.
 * This class belongs to the model layer and contains no UI logic.
 */
public class Notification {

    private final String notificationId;
    private final String entrantId;
    private final String eventId;
    private final String eventTitle; // Added to show user-friendly name instead of ID
    private final String message;
    private final String type;
    private final long sentAtMillis;
    private final boolean read;
    private final String responseStatus;

    /**
     * Backward-compatible constructor for older call sites.
     *
     * @param entrantId    The ID of the entrant receiving the notification.
     * @param eventId      The ID of the event the notification relates to.
     * @param message      The content of the notification message.
     * @param type         The type of notification (e.g., "WIN", "INVITATION").
     * @param sentAtMillis The timestamp when the notification was sent.
     */
    public Notification(String entrantId, String eventId, String message, String type, long sentAtMillis) {
        this(null, entrantId, eventId, null, message, type, sentAtMillis, false, null);
    }

    /**
     * Full constructor for creating a Notification instance with all details.
     *
     * @param notificationId The unique identifier of the notification.
     * @param entrantId      The ID of the entrant receiving the notification.
     * @param eventId        The ID of the event the notification relates to.
     * @param eventTitle     The user-friendly title of the event.
     * @param message        The content of the notification message.
     * @param type           The type of notification.
     * @param sentAtMillis   The timestamp when the notification was sent.
     * @param read           Whether the notification has been read by the user.
     * @param responseStatus The status of the user's response (e.g., "pending", "accepted", "declined").
     */
    public Notification(String notificationId,
                        String entrantId,
                        String eventId,
                        String eventTitle,
                        String message,
                        String type,
                        long sentAtMillis,
                        boolean read,
                        String responseStatus) {
        this.notificationId = notificationId;
        this.entrantId = entrantId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.message = message;
        this.type = type;
        this.sentAtMillis = sentAtMillis;
        this.read = read;
        this.responseStatus = responseStatus;
    }

    /**
     * Gets the unique identifier of the notification.
     *
     * @return The notification ID.
     */
    public String getNotificationId() {
        return notificationId;
    }

    /**
     * Gets the ID of the entrant who received the notification.
     *
     * @return The entrant ID.
     */
    public String getEntrantId() {
        return entrantId;
    }

    /**
     * Gets the ID of the event related to this notification.
     *
     * @return The event ID.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the user-friendly title of the event.
     *
     * @return The event title.
     */
    public String getEventTitle() {
        return eventTitle;
    }

    /**
     * Gets the content message of the notification.
     *
     * @return The message string.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the type of the notification.
     *
     * @return The notification type string.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the timestamp when the notification was sent.
     *
     * @return The time in milliseconds.
     */
    public long getSentAtMillis() {
        return sentAtMillis;
    }

    /**
     * Checks if the notification has been read.
     *
     * @return True if read, false otherwise.
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Gets the current response status for this notification.
     *
     * @return The response status string.
     */
    public String getResponseStatus() {
        return responseStatus;
    }

    /**
     * Determines if this notification is an invitation that requires a response.
     *
     * @return True if it is an invitation type, false otherwise.
     */
    public boolean isInvitation() {
        return type != null && (
                type.equalsIgnoreCase("win")
                        || type.equalsIgnoreCase("invitation")
                        || type.equalsIgnoreCase("selected")
                        || type.equalsIgnoreCase("co_organizer")
                        || type.equalsIgnoreCase("private_invite")
        );
    }

    /**
     * Checks if the invitation can still be accepted (is an invitation and is pending).
     *
     * @return True if it can be accepted.
     */
    public boolean canAcceptInvitation() {
        return isInvitation() && isPendingResponse();
    }

    /**
     * Checks if the invitation can still be declined (is an invitation and is pending).
     *
     * @return True if it can be declined.
     */
    public boolean canDeclineInvitation() {
        return isInvitation() && isPendingResponse();
    }

    /**
     * Checks if the notification is currently awaiting a response.
     *
     * @return True if the status is null, empty, or "pending".
     */
    public boolean isPendingResponse() {
        return responseStatus == null
                || responseStatus.trim().isEmpty()
                || responseStatus.equalsIgnoreCase("pending");
    }
}
