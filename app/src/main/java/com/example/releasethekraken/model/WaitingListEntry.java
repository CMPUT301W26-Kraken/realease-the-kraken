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

    /**
     * create a waiting list entry
     *
     * @param eventId        event identifier
     * @param entrantId      entrant identifier could be device id or user id
     * @param joinedAtMillis time joined
     */
    public WaitingListEntry(String eventId, String entrantId, long joinedAtMillis) {
        this.eventId = eventId;
        this.entrantId = entrantId;
        this.joinedAtMillis = joinedAtMillis;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEntrantId() {
        return entrantId;
    }

    public long getJoinedAtMillis() {
        return joinedAtMillis;
    }

    // Firestore collection structure:
    // All fields are final and populated via the existing constructor.
    // Firestore requires a no-arg constructor, which means the final
    // fields must be explicitly assigned to null/0 inside it.
    // This constructor is only ever called by Firestore — never in app.

    /**
     * No-arg constructor required by Firestore to deserialize document snapshots
     * back into WaitingListEntry objects. Without this, any .toObject(WaitingListEntry.class)
     * call will crash at runtime.
     */
    public WaitingListEntry() {
        this.eventId = null;
        this.entrantId = null;
        this.joinedAtMillis = 0;
    }
}