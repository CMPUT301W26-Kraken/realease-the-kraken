package com.example.releasethekraken.model;

//this is for logging and sending notifications through firebase
//this is in the model package
//no UI logic
//this class is used for US 01.04.01 – Receive win notification
//stores all information related to a notification event,
//such as which entrant received it, which event it relates to,
//the notification message, and the time it was sent


public class Notification {

    private final String entrantId; //unique id for entrant receiving the notification
    private final String eventId; //unique id for the event the notification is for
    private final String message; //notification message
    private final String type; //type of notification (e.g. "win")
    private final long sentAtMillis; //time in milliseconds when the notification was sent


    /**
     * creates a notification object
     *
     * @param entrantId the entrant receiving the notification
     * @param eventId the event related to the notification
     * @param message the message content of the notification
     * @param type the type of notification
     * @param sentAtMillis the timestamp of when the notification was sent
     */
    public Notification(String entrantId, String eventId, String message, String type, long sentAtMillis) {
        this.entrantId = entrantId;
        this.eventId = eventId;
        this.message = message;
        this.type = type;
        this.sentAtMillis = sentAtMillis;

    }
        public String getEntrantId() {
            return entrantId;  //returns entrant id
        }

        public String getEventId() {
            return eventId; //returns event id
        }

        public String getMessage() {
            return message; //returns message
        }

        public String getType() {
            return type; //returns type of message
        }

        public long getSentAtMillis() {
            return sentAtMillis; //returns sent at
        }
    }

