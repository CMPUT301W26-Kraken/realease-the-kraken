package com.example.releasethekraken.model;

import java.util.ArrayList;
import java.util.List;

/**
 * represents an event
 * stores the basic event information such as the event ID, title,
 * description, registration timing, poster metadata, and private-event access metadata.
 */
public class Event {
    public static final int DEFAULT_CAPACITY = 20;

    private final String eventId;
    private final String title;
    private final String description;
    private final long registrationStartMillis;
    private final long registrationEndMillis;
    private final int capacity;
    private final String posterUrl;
    private final boolean isPrivate;
    private final ArrayList<String> invitedUserIds;
    private final ArrayList<String> coOrganizerIds;
    private final String organizerId;
    private final boolean geolocationRequired;

    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                DEFAULT_CAPACITY, "", false, new ArrayList<>(), new ArrayList<>(), "", false);
    }

    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, "", false, new ArrayList<>(), new ArrayList<>(), "", false);
    }

    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 String posterUrl) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, posterUrl, false, new ArrayList<>(), new ArrayList<>(), "", false);
    }

    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 boolean isPrivate, List<String> invitedUserIds, String organizerId) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, "", isPrivate, invitedUserIds, new ArrayList<>(), organizerId, false);
    }

    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 String posterUrl, boolean isPrivate, List<String> invitedUserIds,
                 String organizerId) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, posterUrl, isPrivate, invitedUserIds, new ArrayList<>(), organizerId, false);
    }

    public Event(String eventId, String title, String description,
                 long registrationStartMillis, long registrationEndMillis, int capacity,
                 String posterUrl, boolean isPrivate, List<String> invitedUserIds,
                 String organizerId, boolean geolocationRequired) {
        this(eventId, title, description, registrationStartMillis, registrationEndMillis,
                capacity, posterUrl, isPrivate, invitedUserIds, new ArrayList<>(), organizerId, geolocationRequired);
    }

    /**
     * Full constructor — all other constructors delegate here.
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

    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getRegistrationStartMillis() {
        return registrationStartMillis;
    }

    public long getRegistrationEndMillis() {
        return registrationEndMillis;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public ArrayList<String> getInvitedUserIds() {
        return new ArrayList<>(invitedUserIds);
    }

    public ArrayList<String> getCoOrganizerIds() {
        return new ArrayList<>(coOrganizerIds);
    }

    public String getOrganizerId() {
        return organizerId;
    }
}