package com.example.releasethekraken.controller;

import java.util.ArrayList;
import java.util.List;

import com.example.releasethekraken.model.EventHistoryEntry;

public final class EventHistoryService {
    private EventHistoryService() {
    }

    public static List<EventHistoryEntry> getHistoryForEntrant(
            List<EventHistoryEntry> historyEntries,
            String entrantId
    ) {
        List<EventHistoryEntry> filtered = new ArrayList<>();
        for (EventHistoryEntry entry : historyEntries) {
            if (entry.getEntrantId().equals(entrantId)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
}
