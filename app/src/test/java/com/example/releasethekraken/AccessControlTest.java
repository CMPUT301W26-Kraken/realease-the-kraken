package com.example.releasethekraken;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.releasethekraken.controller.AccessControl;
import com.example.releasethekraken.model.Feature;
import com.example.releasethekraken.model.UserRole;

public class AccessControlTest {

    @Test
    public void entrantHasOnlyEntrantAccess() {
        assertTrue(AccessControl.canAccess(UserRole.ENTRANT, Feature.JOIN_WAITING_LIST));
        assertTrue(AccessControl.canAccess(UserRole.ENTRANT, Feature.FILTER_EVENTS));
        assertTrue(AccessControl.canAccess(UserRole.ENTRANT, Feature.VIEW_EVENT_HISTORY));

        assertFalse(AccessControl.canAccess(UserRole.ENTRANT, Feature.CREATE_EVENT));
        assertFalse(AccessControl.canAccess(UserRole.ENTRANT, Feature.REMOVE_EVENT));
    }

    @Test
    public void organizerHasOnlyOrganizerAccess() {
        assertTrue(AccessControl.canAccess(UserRole.ORGANIZER, Feature.CREATE_EVENT));
        assertTrue(AccessControl.canAccess(UserRole.ORGANIZER, Feature.VIEW_ENTRANTS));
        assertTrue(AccessControl.canAccess(UserRole.ORGANIZER, Feature.DRAW_LOTTERY));

        assertFalse(AccessControl.canAccess(UserRole.ORGANIZER, Feature.FILTER_EVENTS));
        assertFalse(AccessControl.canAccess(UserRole.ORGANIZER, Feature.REMOVE_PROFILE));
    }

    @Test
    public void adminHasOnlyAdminAccess() {
        assertTrue(AccessControl.canAccess(UserRole.ADMIN, Feature.BROWSE_EVENTS));
        assertTrue(AccessControl.canAccess(UserRole.ADMIN, Feature.REMOVE_EVENT));
        assertTrue(AccessControl.canAccess(UserRole.ADMIN, Feature.REMOVE_PROFILE));
        assertTrue(AccessControl.canAccess(UserRole.ADMIN, Feature.REVIEW_NOTIFICATION_LOGS));

        assertFalse(AccessControl.canAccess(UserRole.ADMIN, Feature.DRAW_LOTTERY));
        assertFalse(AccessControl.canAccess(UserRole.ADMIN, Feature.VIEW_EVENT_HISTORY));
    }
}
