package com.example.releasethekraken.model;

/**
 * represents a single entrants entry on an event waiting list
 * It stores only the data related to a waiting list entry and
 * contains no UI
 */


public class WaitingListEntry {

    //the event this entry belongs to
    private final String eventId;

    //the entrant ID that joined the waiting list
    private final String entrantId;

    //when the entrant joined the waiting list in milliseconds
    private final long joinedAtMillis;

    //the latitude coordinate where the entrant joined, 0.0 if not captured
    private final double latitude;

    //the longitude coordinate where the entrant joined, 0.0 if not captured
    private final double longitude;

    /**
     * create a waiting list entry
     *
     * @param eventId        event identifier
     * @param entrantId      entrant identifier could be device id or user id
     * @param joinedAtMillis time joined
     */
    public WaitingListEntry(String eventId, String entrantId, long joinedAtMillis) {
        this(eventId, entrantId, joinedAtMillis, 0.0, 0.0);
    }

    /**
     * create a waiting list entry with location data
     *
     * @param eventId        event identifier
     * @param entrantId      entrant identifier could be device id or user id
     * @param joinedAtMillis time joined
     * @param latitude       latitude where entrant joined
     * @param longitude      longitude where entrant joined
     */
    public WaitingListEntry(String eventId, String entrantId, long joinedAtMillis,
                            double latitude, double longitude) {
        this.eventId = eventId;
        this.entrantId = entrantId;
        this.joinedAtMillis = joinedAtMillis;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getEventId() { //return event id
        return eventId;
    }

    public String getEntrantId() { //return entrant id
        return entrantId;
    }

    public long getJoinedAtMillis() { //return time joined
        return joinedAtMillis;
    }

    public double getLatitude() { //return latitude
        return latitude;
    }

    public double getLongitude() { //return longitude
        return longitude;
    }
}