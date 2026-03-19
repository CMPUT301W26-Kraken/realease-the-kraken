package com.example.releasethekraken;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.releasethekraken.controller.EventFilterService;
import com.example.releasethekraken.controller.SampleDataRepository;
import com.example.releasethekraken.model.Event;
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

    @Test
    public void keywordSearchIsPartialAndCaseInsensitive() {
        List<Event> events = Arrays.asList(
                new Event("1", "Beginner Swimming", "Swim lessons", 100L, 200L, 10),
                new Event("2", "Piano Basics", "Learn piano", 100L, 200L, 25)
        );

        List<Event> filtered = EventFilterService.filterEvents(events, "swim", null, null);

        assertEquals(1, filtered.size());
        assertEquals("Beginner Swimming", filtered.get(0).getTitle());
    }

    @Test
    public void capacityFilterUsesMinimumCapacity() {
        List<Event> events = Arrays.asList(
                new Event("1", "Small Event", "A", 100L, 200L, 10),
                new Event("2", "Large Event", "B", 100L, 200L, 50)
        );

        List<Event> filtered = EventFilterService.filterEvents(events, "", null, 20);

        assertEquals(1, filtered.size());
        assertEquals("Large Event", filtered.get(0).getTitle());
    }

    @Test
    public void availabilityFilterMatchesEventsContainingRequestedTime() {
        List<Event> events = Arrays.asList(
                new Event("1", "Morning Event", "A", 100L, 200L, 10),
                new Event("2", "Evening Event", "B", 300L, 400L, 10)
        );

        List<Event> filtered = EventFilterService.filterEvents(events, "", 150L, null);

        assertEquals(1, filtered.size());
        assertEquals("Morning Event", filtered.get(0).getTitle());
    }

    @Test
    public void combinedFiltersReturnOnlyMatchingEvents() {
        List<Event> events = Arrays.asList(
                new Event("1", "Swimming Lessons", "Kids welcome", 100L, 300L, 30),
                new Event("2", "Swimming Intensive", "Advanced", 100L, 300L, 10),
                new Event("3", "Dance Class", "Beginner", 100L, 300L, 30)
        );

        List<Event> filtered = EventFilterService.filterEvents(events, "swim", 200L, 20);

        assertEquals(1, filtered.size());
        assertEquals("Swimming Lessons", filtered.get(0).getTitle());
    }
}
