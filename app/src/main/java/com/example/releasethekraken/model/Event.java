package com.example.releasethekraken.model;

//event class
// represents an event in the system
// it stores basic information about the event, including its ID, registration start and end times
//needed to determine if an entrant is allowed to join the waiting list for the event

//this is in the model package
//
//no UI logic
public class Event {

    //unique identifier for the event
    //could be a firestore document ID or generated ID
    private final String eventId;

    private final String title; //title of the event
    private final String description; //description of the event

    private final long registrationStartMillis; //time in milliseconds when registration starts for the event
    private final long registrationEndMillis; //time in milliseconds when registration ends for the event

    /**
     * Constructor for creating a new Event object.
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