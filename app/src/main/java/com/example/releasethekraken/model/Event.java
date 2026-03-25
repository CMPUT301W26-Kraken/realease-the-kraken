package com.example.releasethekraken.model;

/**
 * represents an event
 * stores the basic event information such as the event ID, title,
 * description, and registration start and end times
 * this class belongs to the model layer and contains no UI logic
 */
public class Event {
    // Older Firestore documents were created before capacity became part of the model.
    // Keeping a single default in the model avoids scattering the fallback value across
    // the repository, tests, and UI code.
    public static final int DEFAULT_CAPACITY = 20;

    //unique identifier for the event
    //could be a firestore document ID or generated ID
    private final String eventId;

    private final String title; //title of the event
    private final String description; //description of the event

    private final long registrationStartMillis; //time in milliseconds when registration starts for the event
    private final long registrationEndMillis; //time in milliseconds when registration ends for the event
    private final int capacity;
    private final String posterUrl;

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
        // This overload keeps older call sites valid while routing everything through the
        // full constructor so capacity normalization only exists in one place.
        this(eventId, title, description, registrationStartMillis, registrationEndMillis, DEFAULT_CAPACITY, "");
    }

    /**
     * creating a new Event object with an explicit capacity.
     *
     * @param eventId unique ID for the event
     * @param title title of the event
     * @param description description of the event
     * @param registrationStartMillis Time when registration starts
     * @param registrationEndMillis Time when registration ends
     * @param capacity maximum number of entrants the event supports
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis, capacity, "");
    }

    /**
     * creating a new Event object with explicit capacity and poster metadata.
     *
     * @param eventId unique ID for the event
     * @param title title of the event
     * @param description description of the event
     * @param registrationStartMillis Time when registration starts
     * @param registrationEndMillis Time when registration ends
     * @param capacity maximum number of entrants the event supports
     * @param posterUrl remote URL for the event poster image, if one exists
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity, String posterUrl) {
        // Store the raw event metadata exactly as provided by the caller.
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.registrationStartMillis = registrationStartMillis;
        this.registrationEndMillis = registrationEndMillis;
        // Capacity is normalized here instead of in every caller so that older documents,
        // blank create-event input, and invalid values all converge to the same behavior.
        this.capacity = capacity > 0 ? capacity : DEFAULT_CAPACITY;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
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

    //returns capacity
    public int getCapacity() {
        return capacity;
    }

    public String getPosterUrl() {
        return posterUrl;
    }
}
