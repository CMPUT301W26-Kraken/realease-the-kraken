package com.example.releasethekraken;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.releasethekraken.controller.EventFilterService;
import com.example.releasethekraken.controller.SampleDataRepository;
import com.example.releasethekraken.model.FilterEvent;

public class EventFilterServiceTest {

    @Test
    public void filtersByInterestsAndAvailability() {
        List<FilterEvent> events = SampleDataRepository.loadEvents();
        Set<String> interests = new HashSet<>(Arrays.asList("music", "dance"));
        Set<String> availability = new HashSet<>(Arrays.asList("friday", "wednesday"));

        List<FilterEvent> filtered = EventFilterService.filterByInterestsAndAvailability(events, interests, availability);

        assertEquals(2, filtered.size());
        assertEquals("Piano Basics", filtered.get(0).getTitle());
        assertEquals("Interpretive Dance Safety", filtered.get(1).getTitle());
    }

    @Test
    public void emptyFiltersReturnAllEvents() {
        List<FilterEvent> events = SampleDataRepository.loadEvents();

        List<FilterEvent> filtered = EventFilterService.filterByInterestsAndAvailability(
                events,
                new HashSet<>(),
                new HashSet<>()
        );

        assertEquals(events.size(), filtered.size());
    }
}
