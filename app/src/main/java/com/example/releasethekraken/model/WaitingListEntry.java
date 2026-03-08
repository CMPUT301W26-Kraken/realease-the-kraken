package com.example.releasethekraken.model;

//model class to represent one entrants entry on a event waiting list
//this is used for joining wait list and leave waiting list tickets ( US 01.01.01 and  US 01.01.02)
//this class holds data only, no UI or Firebase code added atm.
//


public class WaitingListEntry {

    //the event this entry belongs to
    private final String eventId;

    //the entrant ID that joined the waiting list
    private final String entrantId;

    //when the entrant joined the waiting list in milliseconds
    private final long joinedAtMillis;

    /**
     * Create a waiting list entry
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

    public String getEventId() { //return event id
        return eventId;
    }

    public String getEntrantId() { //return entrant id
        return entrantId;
    }

    public long getJoinedAtMillis() { //return time joined
        return joinedAtMillis;
    }
}
