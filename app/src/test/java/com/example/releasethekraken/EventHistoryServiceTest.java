package com.example.releasethekraken;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

import com.example.releasethekraken.controller.EventHistoryService;
import com.example.releasethekraken.controller.SampleDataRepository;
import com.example.releasethekraken.model.EventHistoryEntry;
import com.example.releasethekraken.model.HistoryOutcome;

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
