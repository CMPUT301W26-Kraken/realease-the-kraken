package com.example.releasethekraken;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class AccessControl {
    private static final Map<Feature, Set<UserRole>> ACCESS_MAP = buildAccessMap();

    private AccessControl() {
    }

    public static boolean canAccess(UserRole role, Feature feature) {
        if (role == null || feature == null) {
            return false;
        }
        Set<UserRole> allowedRoles = ACCESS_MAP.get(feature);
        return allowedRoles != null && allowedRoles.contains(role);
    }

    private static Map<Feature, Set<UserRole>> buildAccessMap() {
        Map<Feature, Set<UserRole>> map = new EnumMap<>(Feature.class);
        map.put(Feature.JOIN_WAITING_LIST, setOf(UserRole.ENTRANT));
        map.put(Feature.FILTER_EVENTS, setOf(UserRole.ENTRANT));
        map.put(Feature.VIEW_EVENT_HISTORY, setOf(UserRole.ENTRANT));

        map.put(Feature.CREATE_EVENT, setOf(UserRole.ORGANIZER));
        map.put(Feature.VIEW_ENTRANTS, setOf(UserRole.ORGANIZER));
        map.put(Feature.DRAW_LOTTERY, setOf(UserRole.ORGANIZER));

        map.put(Feature.BROWSE_EVENTS, setOf(UserRole.ADMIN));
        map.put(Feature.REMOVE_EVENT, setOf(UserRole.ADMIN));
        map.put(Feature.REMOVE_PROFILE, setOf(UserRole.ADMIN));
        map.put(Feature.REVIEW_NOTIFICATION_LOGS, setOf(UserRole.ADMIN));
        return Collections.unmodifiableMap(map);
    }

    private static Set<UserRole> setOf(UserRole... roles) {
        return Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(roles)));
    }
}
