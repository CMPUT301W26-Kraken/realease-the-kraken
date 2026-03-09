package com.example.releasethekraken;

import com.example.releasethekraken.controller.NotificationService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.Notification;
import com.example.releasethekraken.model.NotificationRepository;

import org.junit.Test;

import static org.junit.Assert.*;

public class NotificationServiceTest {

    // fake repository so we can test without Firestore
    static class FakeNotificationRepository extends NotificationRepository {
        Notification sentNotification = null;
        Notification loggedNotification = null;

        @Override
        public void sendNotification(Notification notification) {
            sentNotification = notification;
        }

        @Override
        public void logNotification(Notification notification) {
            loggedNotification = notification;
        }
    }

    @Test
    public void sendWinNotification_invalidInput_returnsInvalidInput() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        NotificationService.NotificationResult result1 =
                service.sendWinNotification(null, "entrant123");
        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result1);

        NotificationService.NotificationResult result2 =
                service.sendWinNotification(makeSampleEvent(), null);
        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result2);

        NotificationService.NotificationResult result3 =
                service.sendWinNotification(makeSampleEvent(), "   ");
        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result3);

        assertNull(repo.sentNotification);
        assertNull(repo.loggedNotification);
    }

    @Test
    public void sendWinNotification_success_sendsAndLogsNotification() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        Event event = makeSampleEvent();
        String entrantId = "entrant123";

        NotificationService.NotificationResult result =
                service.sendWinNotification(event, entrantId);

        assertEquals(NotificationService.NotificationResult.SUCCESS, result);

        // verify notification was sent
        assertNotNull(repo.sentNotification);
        assertEquals(entrantId, repo.sentNotification.getEntrantId());
        assertEquals(event.getEventId(), repo.sentNotification.getEventId());
        assertEquals("WIN", repo.sentNotification.getType());
        assertTrue(repo.sentNotification.getMessage().contains(event.getEventId()));
        assertTrue(repo.sentNotification.getMessage().contains("next steps"));
        assertTrue(repo.sentNotification.getSentAtMillis() > 0);

        // verify notification was logged
        assertNotNull(repo.loggedNotification);
        assertEquals(entrantId, repo.loggedNotification.getEntrantId());
        assertEquals(event.getEventId(), repo.loggedNotification.getEventId());
        assertEquals("WIN", repo.loggedNotification.getType());
    }

    @Test
    public void sendWinNotification_sentAndLoggedNotificationsMatch() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        Event event = makeSampleEvent();
        String entrantId = "entrant123";

        service.sendWinNotification(event, entrantId);

        assertNotNull(repo.sentNotification);
        assertNotNull(repo.loggedNotification);

        assertEquals(repo.sentNotification.getEntrantId(), repo.loggedNotification.getEntrantId());
        assertEquals(repo.sentNotification.getEventId(), repo.loggedNotification.getEventId());
        assertEquals(repo.sentNotification.getMessage(), repo.loggedNotification.getMessage());
        assertEquals(repo.sentNotification.getType(), repo.loggedNotification.getType());
        assertEquals(repo.sentNotification.getSentAtMillis(), repo.loggedNotification.getSentAtMillis());
    }

    // helper method to create a sample event
    private Event makeSampleEvent() {
        long now = System.currentTimeMillis();
        return new Event("event123", now - 1000000, now + 1000000);
    }
}