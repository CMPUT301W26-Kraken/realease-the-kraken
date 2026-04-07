package com.example.releasethekraken.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Represents an event model used specifically for filtering purposes.
 * This class stores event details such as ID, title, tags, and day in a normalized format
 * to facilitate efficient searching and filtering.
 */
public class FilterEvent {
    private final String id;
    private final String title;
    private final Set<String> tags;
    private final String day;

    /**
     * Constructs a new FilterEvent with the specified details.
     * The tags and day are normalized (trimmed and converted to lowercase) during initialization.
     *
     * @param id    The unique identifier for the event.
     * @param title The title of the event.
     * @param tags  A set of tags associated with the event.
     * @param day   The day the event takes place.
     */
    public FilterEvent(String id, String title, Set<String> tags, String day) {
        this.id = id;
        this.title = title;
        this.tags = normalize(tags);
        this.day = day.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Gets the unique identifier of the event.
     *
     * @return The event ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the title of the event.
     *
     * @return The event title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the set of normalized tags associated with the event.
     * The returned set is unmodifiable.
     *
     * @return An unmodifiable set of normalized tags.
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Gets the normalized day of the event.
     *
     * @return The normalized (lowercase and trimmed) day.
     */
    public String getDay() {
        return day;
    }

    /**
     * Normalizes a set of strings by trimming them, converting to lowercase,
     * and removing null or empty values.
     *
     * @param values The set of strings to normalize.
     * @return An unmodifiable set of normalized strings.
     */
    private Set<String> normalize(Set<String> values) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                normalized.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(normalized);
    }
}
