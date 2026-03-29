package com.example.releasethekraken.model;

import java.util.ArrayList;
import java.util.List;

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
    private final boolean isPrivate;
    private final ArrayList<String> invitedUserIds;
    private final String organizerId;

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
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                DEFAULT_CAPACITY, false, new ArrayList<>(), "");
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
        // Store the raw event metadata exactly as provided by the caller.
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, false, new ArrayList<>(), "");
    }

    /**
     * creating a new Event object with explicit capacity, visibility, invited users, and organizer.
     *
     * @param eventId unique ID for the event
     * @param title title of the event
     * @param description description of the event
     * @param registrationStartMillis Time when registration starts
     * @param registrationEndMillis Time when registration ends
     * @param capacity maximum number of entrants the event supports
     * @param isPrivate true if the event is private, false if it is public
     * @param invitedUserIds list of invited user IDs for private events
     * @param organizerId unique ID of the organizer who created the event
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 boolean isPrivate, List<String> invitedUserIds, String organizerId) {
        // Store the raw event metadata exactly as provided by the caller.
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.registrationStartMillis = registrationStartMillis;
        this.registrationEndMillis = registrationEndMillis;
        // Capacity is normalized here instead of in every caller so that older documents,
        // blank create-event input, and invalid values all converge to the same behavior.
        this.capacity = capacity > 0 ? capacity : DEFAULT_CAPACITY;
        this.isPrivate = isPrivate;
        // Store invited users in a non-null list so the rest of the app does not need
        // repeated null checks when validating access to private events.
        this.invitedUserIds = invitedUserIds == null ? new ArrayList<>() : new ArrayList<>(invitedUserIds);
        // Organizer ID defaults to an empty string so older call sites and older documents
        // do not break while private-event support is being added.
        this.organizerId = organizerId == null ? "" : organizerId;
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

    //returns whether the event is private
    public boolean isPrivate() {
        return isPrivate;
    }

    //returns the invited user IDs for the event
    public ArrayList<String> getInvitedUserIds() {
        return new ArrayList<>(invitedUserIds);
    }

    //returns the organizer ID for the event
    public String getOrganizerId() {
        return organizerId;
    }
}