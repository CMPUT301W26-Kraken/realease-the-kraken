package com.example.releasethekraken.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.example.releasethekraken.model.EventHistoryEntry;
import com.example.releasethekraken.model.FilterEvent;
import com.example.releasethekraken.model.HistoryOutcome;

public final class SampleDataRepository {
    private SampleDataRepository() {
    }

    public static List<FilterEvent> loadEvents() {
        List<FilterEvent> events = new ArrayList<>();
        events.add(new FilterEvent("e1", "Beginner Swimming", set("swimming", "fitness", "kids"), "monday"));
        events.add(new FilterEvent("e2", "Piano Basics", set("music", "piano", "beginner"), "wednesday"));
        events.add(new FilterEvent("e3", "Interpretive Dance Safety", set("dance", "safety"), "friday"));
        events.add(new FilterEvent("e4", "Weekend Canoeing", set("outdoors", "canoe"), "saturday"));
        return events;
    }

    public static List<EventHistoryEntry> loadEventHistory() {
        List<EventHistoryEntry> history = new ArrayList<>();
        history.add(new EventHistoryEntry("entrant_device_1", "Beginner Swimming", HistoryOutcome.NOT_SELECTED));
        history.add(new EventHistoryEntry("entrant_device_1", "Piano Basics", HistoryOutcome.SELECTED));
        history.add(new EventHistoryEntry("entrant_device_2", "Weekend Canoeing", HistoryOutcome.WAITING));
        history.add(new EventHistoryEntry("entrant_device_1", "Interpretive Dance Safety", HistoryOutcome.DECLINED));
        return history;
    }

    private static HashSet<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
