package com.example.releasethekraken.controller;


import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.Notification;
import com.example.releasethekraken.model.NotificationRepository;


//handles notification logic
//this class belongs to the controller layer and contains the rules
//for creating and sending notifications to entrants
//the result is a notification message, sends the notification, and logs the notification event
//This class does not contain UI code and does not directly interact with Firestore
//instead it delegates storage and delivery to NotificationRepository which is what interacts with Firestore


public class NotificationService {
    //repository responsible for sending and logging notifications
    //this will later connect to Firestore
    private final NotificationRepository notificationRepository;

    //constructor that injects the repository dependency
    //this allows the service to interact with the data layer
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    //possible outcomes when attempting to send a notification
    //this makes it easier for the view layer to determine what happened
    public enum NotificationResult {
        SUCCESS,
        INVALID_INPUT
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
    public NotificationResult sendWinNotification(Event event, String entrantId) {

        //validate inputs to prevent null values or empty entrant identifiers
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            return NotificationResult.INVALID_INPUT;
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

        //trigger the notification through the repository
        //this will eventually send the notification through the app or backend
        notificationRepository.sendNotification(notification);

        //log the notification event so it can be stored in Firestore
        notificationRepository.logNotification(notification);

        //indicate the notification was successfully processed
        return NotificationResult.SUCCESS;
    }
}

