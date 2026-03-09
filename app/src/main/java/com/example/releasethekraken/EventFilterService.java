package com.example.releasethekraken;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class EventFilterService {
    private EventFilterService() {
    }

    public static List<Event> filterByInterestsAndAvailability(
            List<Event> events,
            Set<String> interests,
            Set<String> availableDays
    ) {
        List<Event> matches = new ArrayList<>();
        for (Event event : events) {
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

    private static boolean matchesAvailability(Event event, Set<String> availableDays) {
        if (availableDays == null || availableDays.isEmpty()) {
            return true;
        }
        return availableDays.contains(event.getDay().toLowerCase(Locale.ROOT));
    }

    private static boolean matchesInterests(Event event, Set<String> interests) {
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
