package com.example.releasethekraken;

public enum HistoryOutcome {
    SELECTED("Selected"),
    NOT_SELECTED("Not selected"),
    DECLINED("Declined"),
    WAITING("Waiting");

    private final String label;

    HistoryOutcome(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
