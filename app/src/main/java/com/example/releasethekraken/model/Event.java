package com.example.releasethekraken.model;

/**
 * represents an event
 * stores the basic event information such as the event ID, title,
 * description, and registration start and end times
 * this class belongs to the model layer and contains no UI logic
 */
public class Event {

    //unique identifier for the event
    //could be a firestore document ID or generated ID
    private final String eventId;

    private final String title; //title of the event
    private final String description; //description of the event

    private final long registrationStartMillis; //time in milliseconds when registration starts for the event
    private final long registrationEndMillis; //time in milliseconds when registration ends for the event

    /**
     *  creating a new Event object
     *
     * @param eventId unique ID for the event
     * @param title title of the event
     * @param description description of the event
     * @param registrationStartMillis Time when registration starts
     * @param registrationEndMillis Time when registration ends
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis) {
        //assign constructor parameters to class fields
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.registrationStartMillis = registrationStartMillis;
        this.registrationEndMillis = registrationEndMillis;
    }

    //returns a unique ID for the event
    public String getEventId() {
        return eventId;
    }

    //returns event title
    public String getTitle() {
        return title;
    }

    //returns event description
    public String getDescription() {
        return description;
    }

    //returns registration start time
    public long getRegistrationStartMillis() {
        return registrationStartMillis;
    }

    //returns registration end time
    public long getRegistrationEndMillis() {
        return registrationEndMillis;
    }
}