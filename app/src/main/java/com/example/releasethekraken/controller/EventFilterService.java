package com.example.releasethekraken.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.FilterEvent;

public final class EventFilterService {
    private EventFilterService() {
    }

    /**
     * Applies every active browse-events constraint to the in-memory event list.
     *
     * We filter locally after loading from Firestore because keyword matching needs partial,
     * case-insensitive checks across title and description, which does not map cleanly to the
     * current Firestore schema.
     */
    public static List<Event> filterEvents(
            List<Event> events,
            String keyword,
            Long availableAtMillis,
            Integer minimumCapacity
    ) {
        List<Event> matches = new ArrayList<>();
        for (Event event : events) {
            // Skip as soon as one predicate fails so the combined search/filter flow remains
            // straightforward and cheap for small browse lists.
            if (!matchesKeyword(event, keyword)) {
                continue;
            }
            if (!matchesAvailability(event, availableAtMillis)) {
                continue;
            }
            if (!matchesCapacity(event, minimumCapacity)) {
                continue;
            }
            matches.add(event);
        }
        return matches;
    }

    /**
     * Legacy/sample filtering path kept for the earlier ticket-test flow and unit tests.
     * This operates on FilterEvent, not the real Firestore-backed Event model.
     */
    public static List<FilterEvent> filterByInterestsAndAvailability(
            List<FilterEvent> events,
            Set<String> interests,
            Set<String> availableDays
    ) {
        List<FilterEvent> matches = new ArrayList<>();
        for (FilterEvent event : events) {
            if (!matchesAvailability(event, availableDays)) {
                continue;
            }
            if (!matchesInterests(event, interests)) {
                continue;
            }
            matches.add(event);
        }
        return matches;
    }

    private static boolean matchesKeyword(Event event, String keyword) {
        // Empty keyword means "do not constrain results by text".
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        // Normalize both sides so search works with partial input regardless of case.
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT).trim();
        return event.getTitle().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || event.getDescription().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private static boolean matchesAvailability(Event event, Long availableAtMillis) {
        // No availability filter means keep the event.
        if (availableAtMillis == null) {
            return true;
        }
        // The current Event model only stores the registration availability window, so the
        // browse filter interprets "available at" as "registration is open at this time".
        return availableAtMillis >= event.getRegistrationStartMillis()
                && availableAtMillis <= event.getRegistrationEndMillis();
    }

    private static boolean matchesCapacity(Event event, Integer minimumCapacity) {
        // Null means the user did not request a minimum capacity threshold.
        if (minimumCapacity == null) {
            return true;
        }
        // Capacity filtering is inclusive: asking for 20 should keep events with capacity 20+.
        return event.getCapacity() >= minimumCapacity;
    }

    private static boolean matchesAvailability(FilterEvent event, Set<String> availableDays) {
        if (availableDays == null || availableDays.isEmpty()) {
            return true;
        }
        // FilterEvent uses weekday strings rather than timestamps, so the comparison is done
        // against the normalized stored day value.
        return availableDays.contains(event.getDay().toLowerCase(Locale.ROOT));
    }

    private static boolean matchesInterests(FilterEvent event, Set<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return true;
        }
        for (String interest : interests) {
            // Old sample data uses lowercase tags, so normalize each requested interest before
            // comparing to keep behavior consistent with the real keyword search path.
            if (event.getTags().contains(interest.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
