package com.example.releasethekraken;

import java.util.ArrayList;
import java.util.List;

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
