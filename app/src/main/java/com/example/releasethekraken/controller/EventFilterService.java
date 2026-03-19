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

    public static List<Event> filterEvents(
            List<Event> events,
            String keyword,
            Long availableAtMillis,
            Integer minimumCapacity
    ) {
        List<Event> matches = new ArrayList<>();
        for (Event event : events) {
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
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT).trim();
        return event.getTitle().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || event.getDescription().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private static boolean matchesAvailability(Event event, Long availableAtMillis) {
        if (availableAtMillis == null) {
            return true;
        }
        // The current Event model only stores the registration availability window.
        return availableAtMillis >= event.getRegistrationStartMillis()
                && availableAtMillis <= event.getRegistrationEndMillis();
    }

    private static boolean matchesCapacity(Event event, Integer minimumCapacity) {
        if (minimumCapacity == null) {
            return true;
        }
        return event.getCapacity() >= minimumCapacity;
    }

    private static boolean matchesAvailability(FilterEvent event, Set<String> availableDays) {
        if (availableDays == null || availableDays.isEmpty()) {
            return true;
        }
        return availableDays.contains(event.getDay().toLowerCase(Locale.ROOT));
    }

    private static boolean matchesInterests(FilterEvent event, Set<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return true;
        }
        for (String interest : interests) {
            if (event.getTags().contains(interest.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
