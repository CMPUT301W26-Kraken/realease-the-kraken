package com.example.releasethekraken.model;

/**
 * represents a single entry in an entrant's event history
 * stores information about which event the entrant participated in
 * and the outcome of that participation (e.g., selected, not selected)
 * this class belongs to the model layer and contains no UI logic
 */
public class EventHistoryEntry {

    // unique identifier for the entrant
    private final String entrantId;

    // title of the event associated with this history entry
    private final String eventTitle;

    // outcome of the entrant's participation in the event
    private final HistoryOutcome outcome;

    /**
     * creates a new EventHistoryEntry object
     *
     * @param entrantId unique identifier for the entrant
     * @param eventTitle title of the event
     * @param outcome result of the entrant's participation
     */
    public EventHistoryEntry(String entrantId, String eventTitle, HistoryOutcome outcome) {
        this.entrantId = entrantId;
        this.eventTitle = eventTitle;
        this.outcome = outcome;
    }

    /**
     * gets the entrant ID associated with this history entry
     *
     * @return entrant ID as a String
     */
    public String getEntrantId() {
        return entrantId;
    }

    /**
     * gets the title of the event for this history entry
     *
     * @return event title as a String
     */
    public String getEventTitle() {
        return eventTitle;
    }

    /**
     * gets the outcome of the entrant's participation
     *
     * @return HistoryOutcome representing the result
     */
    public HistoryOutcome getOutcome() {
        return outcome;
    }
}