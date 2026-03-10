package com.example.releasethekraken.model;

public enum Feature {
    JOIN_WAITING_LIST("Join waiting list"),
    FILTER_EVENTS("Filter events"),
    VIEW_EVENT_HISTORY("View event history"),
    CREATE_EVENT("Create event"),
    VIEW_ENTRANTS("View entrants"),
    DRAW_LOTTERY("Draw lottery"),
    BROWSE_EVENTS("Browse events"),
    REMOVE_EVENT("Remove event"),
    REMOVE_PROFILE("Remove profile"),
    REVIEW_NOTIFICATION_LOGS("Review notification logs");

    private final String label;

    Feature(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
