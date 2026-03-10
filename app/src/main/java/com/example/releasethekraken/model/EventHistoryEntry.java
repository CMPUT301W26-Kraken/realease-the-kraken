package com.example.releasethekraken.model;

public class EventHistoryEntry {
    private final String entrantId;
    private final String eventTitle;
    private final HistoryOutcome outcome;

    public EventHistoryEntry(String entrantId, String eventTitle, HistoryOutcome outcome) {
        this.entrantId = entrantId;
        this.eventTitle = eventTitle;
        this.outcome = outcome;
    }

    public String getEntrantId() {
        return entrantId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public HistoryOutcome getOutcome() {
        return outcome;
    }
}
