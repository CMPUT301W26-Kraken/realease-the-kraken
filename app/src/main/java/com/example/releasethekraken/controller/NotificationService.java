package com.example.releasethekraken.controller;

import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.Notification;
import com.example.releasethekraken.model.NotificationRepository;

/**
 * service responsible for handling notification business logic
 * This class creates notification messages for entrants, sends the
 * notifications, and logs notification events. It belongs to the
 * controller layer and contains no UI code
 * storage and delivery of notifications are delegated to
 * notificationRepository, which interacts with Firestore
 */

public class NotificationService {

    /** repository responsible for sending and logging notifications. */
    private final NotificationRepository notificationRepository;

    /**
     * creates a NotificationService with the given repository dependency
     * @param notificationRepository repository used for notification storage and delivery
     */
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * possible outcomes when attempting to send a notification
     */
    public enum NotificationResult {
        SUCCESS,
        INVALID_INPUT
    }
    /**
     * callback interface used to return notification results.
     */
    public interface NotificationCallback {
        void onResult(NotificationResult result);
        void onError(Exception e);
    }

    /**
     * sends a win notification to an entrant
     * responsibilities:
     *  - validate input
     *  - generate the notification message
     *  - create a Notification object
     *  - trigger the notification through the repository
     *  - log the notification event
     *
     * @param event the event the entrant was selected for
     * @param entrantId the entrant receiving the notification
     * @return NotificationResult indicating success or invalid input
     */
    public void sendWinNotification(Event event, String entrantId, NotificationCallback callback) {

        //validate inputs to prevent null values or empty entrant identifiers
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            callback.onResult(NotificationResult.INVALID_INPUT);
            return;
        }

        //capture the exact time the notification is being created
        //this timestamp can later be used for logging or display purposes
        long nowMillis = System.currentTimeMillis();

        //create a message informing the entrant that they won
        //the message states the event and instructs the entrant
        //to check the app for next steps
        String message = "Congratulations! You have been selected for event "
                + event.getEventId()
                + ". Please check the app for next steps to complete your registration.";

        //create a notification object containing all required information
        Notification notification = new Notification(
                entrantId,
                event.getEventId(),
                message,
                "WIN",
                nowMillis
        );

        sendAndLogNotification(notification, callback);
    }

    /**
     * sends a loss notification to an entrant
     * responsibilities:
     *  - validate input
     *  - generate the notification message
     *  - create a Notification object
     *  - trigger the notification through the repository
     *  - log the notification event
     *
     * @param event the event the entrant was not selected for
     * @param entrantId the entrant receiving the notification
     */
    public void sendLossNotification(Event event, String entrantId, NotificationCallback callback) {

        //validate inputs to prevent null values or empty entrant identifiers
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            callback.onResult(NotificationResult.INVALID_INPUT);
            return;
        }

        //capture the exact time the notification is being created
        long nowMillis = System.currentTimeMillis();

        //create a message informing the entrant that they were not selected
        String message = "Thank you for your interest in event "
                + event.getEventId()
                + ". You were not selected in this draw.";

        //create a notification object containing all required information
        Notification notification = new Notification(
                entrantId,
                event.getEventId(),
                message,
                "LOSS",
                nowMillis
        );

        sendAndLogNotification(notification, callback);
    }

    /**
     * sends an invitation notification to a selected entrant.
     *
     * @param event the event the entrant was selected for
     * @param entrantId the entrant receiving the notification
     * @param organizerMessage optional organizer-authored note with next steps
     * @param callback callback returning notification outcome
     */
    public void sendSelectedEntrantNotification(
            Event event,
            String entrantId,
            String organizerMessage,
            NotificationCallback callback
    ) {
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            callback.onResult(NotificationResult.INVALID_INPUT);
            return;
        }

        long nowMillis = System.currentTimeMillis();
        String trimmedMessage = organizerMessage == null ? "" : organizerMessage.trim();
        String eventName = event.getTitle() == null || event.getTitle().trim().isEmpty()
                ? event.getEventId()
                : event.getTitle();

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("You have been invited to sign up for ")
                .append(eventName)
                .append(". Please check the app and complete your registration.");

        if (!trimmedMessage.isEmpty()) {
            messageBuilder.append(" Organizer note: ").append(trimmedMessage);
        }

        Notification notification = new Notification(
                entrantId,
                event.getEventId(),
                messageBuilder.toString(),
                "SELECTED",
                nowMillis
        );

        sendAndLogNotification(notification, callback);
    }

    private void sendAndLogNotification(Notification notification, NotificationCallback callback) {
        notificationRepository.sendNotification(notification, new NotificationRepository.CompletionCallback() {
            @Override
            public void onSuccess() {
                notificationRepository.logNotification(notification, new NotificationRepository.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onResult(NotificationResult.SUCCESS);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
}
