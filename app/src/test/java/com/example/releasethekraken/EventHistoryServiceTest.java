package com.example.releasethekraken;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class EventHistoryServiceTest {

    @Test
    public void returnsOnlyRequestedEntrantHistory() {
        List<EventHistoryEntry> history = SampleDataRepository.loadEventHistory();

        List<EventHistoryEntry> filtered = EventHistoryService.getHistoryForEntrant(history, "entrant_device_1");

        assertEquals(3, filtered.size());
        assertEquals("Beginner Swimming", filtered.get(0).getEventTitle());
        assertEquals(HistoryOutcome.NOT_SELECTED, filtered.get(0).getOutcome());
    }
}
