package com.example.releasethekraken.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event within the system.
 * This class stores essential event information including identification, title,
 * description, registration timeframes, capacity, poster metadata, and access control settings.
 */
public class Event {
    /**
     * The default capacity for an event if none is specified.
     */
    public static final int DEFAULT_CAPACITY = 20;

    private final String eventId;
    private final String title;
    private final String description;
    private final long registrationStartMillis;
    private final long registrationEndMillis;
    private final int capacity;
    private String posterUrl;
    private final boolean isPrivate;
    private final ArrayList<String> invitedUserIds;
    private final ArrayList<String> coOrganizerIds;
    private final String organizerId;
    private final boolean geolocationRequired;

    /**
     * Constructs an Event with basic timing details and default capacity.
     *
     * @param eventId                 The unique identifier for the event.
     * @param title                   The title of the event.
     * @param description             A detailed description of the event.
     * @param registrationStartMillis The timestamp (in milliseconds) when registration begins.
     * @param registrationEndMillis   The timestamp (in milliseconds) when registration ends.
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                DEFAULT_CAPACITY, "", false, new ArrayList<>(), new ArrayList<>(), "", false);
    }

    /**
     * Constructs an Event with a specified capacity and timing details.
     *
     * @param eventId                 The unique identifier for the event.
     * @param title                   The title of the event.
     * @param description             A detailed description of the event.
     * @param registrationStartMillis The timestamp (in milliseconds) when registration begins.
     * @param registrationEndMillis   The timestamp (in milliseconds) when registration ends.
     * @param capacity                The maximum number of entrants allowed for the event.
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, "", false, new ArrayList<>(), new ArrayList<>(), "", false);
    }

    /**
     * Constructs an Event with a poster URL, capacity, and timing details.
     *
     * @param eventId                 The unique identifier for the event.
     * @param title                   The title of the event.
     * @param description             A detailed description of the event.
     * @param registrationStartMillis The timestamp (in milliseconds) when registration begins.
     * @param registrationEndMillis   The timestamp (in milliseconds) when registration ends.
     * @param capacity                The maximum number of entrants allowed for the event.
     * @param posterUrl               The URL for the event's promotional poster.
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 String posterUrl) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, posterUrl, false, new ArrayList<>(), new ArrayList<>(), "", false);
    }

    /**
     * Constructs an Event with privacy settings and initial invited users.
     *
     * @param eventId                 The unique identifier for the event.
     * @param title                   The title of the event.
     * @param description             A detailed description of the event.
     * @param registrationStartMillis The timestamp (in milliseconds) when registration begins.
     * @param registrationEndMillis   The timestamp (in milliseconds) when registration ends.
     * @param capacity                The maximum number of entrants allowed for the event.
     * @param isPrivate               Whether the event is private (requires an invite).
     * @param invitedUserIds          A list of user IDs who are invited to this event.
     * @param organizerId             The ID of the user who organized the event.
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 boolean isPrivate, List<String> invitedUserIds, String organizerId) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, "", isPrivate, invitedUserIds, new ArrayList<>(), organizerId, false);
    }

    /**
     * Constructs an Event with poster URL, privacy settings, and invited users.
     *
     * @param eventId                 The unique identifier for the event.
     * @param title                   The title of the event.
     * @param description             A detailed description of the event.
     * @param registrationStartMillis The timestamp (in milliseconds) when registration begins.
     * @param registrationEndMillis   The timestamp (in milliseconds) when registration ends.
     * @param capacity                The maximum number of entrants allowed for the event.
     * @param posterUrl               The URL for the event's promotional poster.
     * @param isPrivate               Whether the event is private.
     * @param invitedUserIds          A list of user IDs who are invited to this event.
     * @param organizerId             The ID of the user who organized the event.
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 String posterUrl, boolean isPrivate, List<String> invitedUserIds,
                 String organizerId) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, posterUrl, isPrivate, invitedUserIds, new ArrayList<>(), organizerId, false);
    }

    /**
     * Constructs an Event with geolocation requirement and other details.
     *
     * @param eventId                 The unique identifier for the event.
     * @param title                   The title of the event.
     * @param description             A detailed description of the event.
     * @param registrationStartMillis The timestamp (in milliseconds) when registration begins.
     * @param registrationEndMillis   The timestamp (in milliseconds) when registration ends.
     * @param capacity                The maximum number of entrants allowed for the event.
     * @param posterUrl               The URL for the event's promotional poster.
     * @param isPrivate               Whether the event is private.
     * @param invitedUserIds          A list of user IDs who are invited to this event.
     * @param organizerId             The ID of the user who organized the event.
     * @param geolocationRequired     Whether entrants must provide their location to join.
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 String posterUrl, boolean isPrivate, List<String> invitedUserIds,
                 String organizerId, boolean geolocationRequired) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, posterUrl, isPrivate, invitedUserIds, new ArrayList<>(), organizerId, geolocationRequired);
    }

    /**
     * Full constructor — all other constructors delegate here.
     *
     * @param eventId                 The unique identifier for the event.
     * @param title                   The title of the event.
     * @param description             A detailed description of the event.
     * @param registrationStartMillis The timestamp when registration begins.
     * @param registrationEndMillis   The timestamp when registration ends.
     * @param capacity                The maximum capacity of the event.
     * @param posterUrl               The URL of the event poster image.
     * @param isPrivate               True if the event is restricted to invited users.
     * @param invitedUserIds          List of IDs for users specifically invited.
     * @param coOrganizerIds          List of IDs for users acting as co-organizers.
     * @param organizerId             The ID of the primary organizer.
     * @param geolocationRequired     True if entrants must share location data.
     */
    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 String posterUrl, boolean isPrivate, List<String> invitedUserIds,
                 List<String> coOrganizerIds, String organizerId, boolean geolocationRequired) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.registrationStartMillis = registrationStartMillis;
        this.registrationEndMillis = registrationEndMillis;
        this.capacity = capacity > 0 ? capacity : DEFAULT_CAPACITY;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
        this.isPrivate = isPrivate;
        this.invitedUserIds = invitedUserIds == null ? new ArrayList<>() : new ArrayList<>(invitedUserIds);
        this.coOrganizerIds = coOrganizerIds == null ? new ArrayList<>() : new ArrayList<>(coOrganizerIds);
        this.organizerId = organizerId == null ? "" : organizerId;
        this.geolocationRequired = geolocationRequired;
    }

    /**
     * Checks if geolocation is required for this event.
     *
     * @return True if geolocation is required, false otherwise.
     */
    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Gets the unique ID of the event.
     *
     * @return The event ID.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the title of the event.
     *
     * @return The event title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the description of the event.
     *
     * @return The event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the registration start time in milliseconds.
     *
     * @return The registration start time.
     */
    public long getRegistrationStartMillis() {
        return registrationStartMillis;
    }

    /**
     * Gets the registration end time in milliseconds.
     *
     * @return The registration end time.
     */
    public long getRegistrationEndMillis() {
        return registrationEndMillis;
    }

    /**
     * Gets the maximum capacity of the event.
     *
     * @return The event capacity.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Gets the URL of the event poster image.
     *
     * @return The poster URL.
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Sets the URL of the event poster image.
     *
     * @param posterUrl The new poster URL.
     */
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl == null ? "" : posterUrl;
    }

    /**
     * Checks if the event is private.
     *
     * @return True if private, false if public.
     */
    public boolean isPrivate() {
        return isPrivate;
    }

    /**
     * Returns a list of IDs of users invited to this event.
     *
     * @return A list of invited user IDs.
     */
    public ArrayList<String> getInvitedUserIds() {
        return new ArrayList<>(invitedUserIds);
    }

    /**
     * Returns a list of IDs of users who are co-organizers of this event.
     *
     * @return A list of co-organizer IDs.
     */
    public ArrayList<String> getCoOrganizerIds() {
        return new ArrayList<>(coOrganizerIds);
    }

    /**
     * Gets the ID of the primary organizer of the event.
     *
     * @return The organizer ID.
     */
    public String getOrganizerId() {
        return organizerId;
    }
}
