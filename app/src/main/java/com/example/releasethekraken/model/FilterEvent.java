package com.example.releasethekraken.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FilterEvent {
    private final String id;
    private final String title;
    private final Set<String> tags;
    private final String day;

    public FilterEvent(String id, String title, Set<String> tags, String day) {
        this.id = id;
        this.title = title;
        this.tags = normalize(tags);
        this.day = day.trim().toLowerCase(Locale.ROOT);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getDay() {
        return day;
    }

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
