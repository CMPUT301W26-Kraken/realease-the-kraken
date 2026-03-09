package com.example.releasethekraken;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventFilterServiceTest {

    @Test
    public void filtersByInterestsAndAvailability() {
        List<Event> events = SampleDataRepository.loadEvents();
        Set<String> interests = new HashSet<>(Arrays.asList("music", "dance"));
        Set<String> availability = new HashSet<>(Arrays.asList("friday", "wednesday"));

        List<Event> filtered = EventFilterService.filterByInterestsAndAvailability(events, interests, availability);

        assertEquals(2, filtered.size());
        assertEquals("Piano Basics", filtered.get(0).getTitle());
        assertEquals("Interpretive Dance Safety", filtered.get(1).getTitle());
    }

    @Test
    public void emptyFiltersReturnAllEvents() {
        List<Event> events = SampleDataRepository.loadEvents();

        List<Event> filtered = EventFilterService.filterByInterestsAndAvailability(
                events,
                new HashSet<>(),
                new HashSet<>()
        );

        assertEquals(events.size(), filtered.size());
    }
}
