package com.example.releasethekraken;

import com.example.releasethekraken.controller.NotificationService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.Notification;
import com.example.releasethekraken.model.NotificationRepository;

import org.junit.Test;

import static org.junit.Assert.*;
/**
 * Unit tests for NotificationService.
 */
public class NotificationServiceTest {

    // fake repository so we can test without Firestore
    static class FakeNotificationRepository extends NotificationRepository {
        Notification sentNotification = null;
        Notification loggedNotification = null;

        public FakeNotificationRepository() {
            super(null);
        }

        @Override
        public void sendNotification(Notification notification, CompletionCallback callback) {
            sentNotification = notification;
            callback.onSuccess();
        }

        @Override
        public void logNotification(Notification notification, CompletionCallback callback) {
            loggedNotification = notification;
            callback.onSuccess();
        }
    }

    @Test
    public void sendWinNotification_invalidInput_returnsInvalidInput() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        final NotificationService.NotificationResult[] result1 = new NotificationService.NotificationResult[1];

        service.sendWinNotification(null, "entrant123", new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result1[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result1[0]);

        final NotificationService.NotificationResult[] result2 = new NotificationService.NotificationResult[1];

        service.sendWinNotification(makeSampleEvent(), null, new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result2[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result2[0]);

        final NotificationService.NotificationResult[] result3 = new NotificationService.NotificationResult[1];

        service.sendWinNotification(makeSampleEvent(), "   ", new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result3[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result3[0]);

        assertNull(repo.sentNotification);
        assertNull(repo.loggedNotification);
    }

    @Test
    public void sendWinNotification_success_sendsAndLogsNotification() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        Event event = makeSampleEvent();
        String entrantId = "entrant123";

        final NotificationService.NotificationResult[] result = new NotificationService.NotificationResult[1];

        service.sendWinNotification(event, entrantId, new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.SUCCESS, result[0]);

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

        service.sendWinNotification(event, entrantId, new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                // no extra action needed
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertNotNull(repo.sentNotification);
        assertNotNull(repo.loggedNotification);

        assertEquals(repo.sentNotification.getEntrantId(), repo.loggedNotification.getEntrantId());
        assertEquals(repo.sentNotification.getEventId(), repo.loggedNotification.getEventId());
        assertEquals(repo.sentNotification.getMessage(), repo.loggedNotification.getMessage());
        assertEquals(repo.sentNotification.getType(), repo.loggedNotification.getType());
        assertEquals(repo.sentNotification.getSentAtMillis(), repo.loggedNotification.getSentAtMillis());
    }

    @Test
    public void sendLossNotification_invalidInput_returnsInvalidInput() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        final NotificationService.NotificationResult[] result1 = new NotificationService.NotificationResult[1];

        service.sendLossNotification(null, "entrant123", new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result1[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result1[0]);

        final NotificationService.NotificationResult[] result2 = new NotificationService.NotificationResult[1];

        service.sendLossNotification(makeSampleEvent(), null, new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result2[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result2[0]);

        final NotificationService.NotificationResult[] result3 = new NotificationService.NotificationResult[1];

        service.sendLossNotification(makeSampleEvent(), "   ", new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result3[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.INVALID_INPUT, result3[0]);

        assertNull(repo.sentNotification);
        assertNull(repo.loggedNotification);
    }

    @Test
    public void sendLossNotification_success_sendsAndLogsNotification() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        Event event = makeSampleEvent();
        String entrantId = "entrant123";

        final NotificationService.NotificationResult[] result = new NotificationService.NotificationResult[1];

        service.sendLossNotification(event, entrantId, new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.SUCCESS, result[0]);

        // verify notification was sent
        assertNotNull(repo.sentNotification);
        assertEquals(entrantId, repo.sentNotification.getEntrantId());
        assertEquals(event.getEventId(), repo.sentNotification.getEventId());
        assertEquals("LOSS", repo.sentNotification.getType());
        assertTrue(repo.sentNotification.getMessage().contains(event.getEventId()));
        assertTrue(repo.sentNotification.getMessage().contains("not selected"));
        assertTrue(repo.sentNotification.getSentAtMillis() > 0);

        // verify notification was logged
        assertNotNull(repo.loggedNotification);
        assertEquals(entrantId, repo.loggedNotification.getEntrantId());
        assertEquals(event.getEventId(), repo.loggedNotification.getEventId());
        assertEquals("LOSS", repo.loggedNotification.getType());
    }

    @Test
    public void sendLossNotification_sentAndLoggedNotificationsMatch() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        Event event = makeSampleEvent();
        String entrantId = "entrant123";

        service.sendLossNotification(event, entrantId, new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                // no extra action needed
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertNotNull(repo.sentNotification);
        assertNotNull(repo.loggedNotification);

        assertEquals(repo.sentNotification.getEntrantId(), repo.loggedNotification.getEntrantId());
        assertEquals(repo.sentNotification.getEventId(), repo.loggedNotification.getEventId());
        assertEquals(repo.sentNotification.getMessage(), repo.loggedNotification.getMessage());
        assertEquals(repo.sentNotification.getType(), repo.loggedNotification.getType());
        assertEquals(repo.sentNotification.getSentAtMillis(), repo.loggedNotification.getSentAtMillis());
    }

    @Test
    public void sendSelectedEntrantNotification_success_sendsAndLogsNotification() {
        FakeNotificationRepository repo = new FakeNotificationRepository();
        NotificationService service = new NotificationService(repo);

        Event event = makeSampleEvent();
        String entrantId = "entrant123";
        final NotificationService.NotificationResult[] result = new NotificationService.NotificationResult[1];

        service.sendSelectedEntrantNotification(event, entrantId, "Bring your ID", new NotificationService.NotificationCallback() {
            @Override
            public void onResult(NotificationService.NotificationResult notificationResult) {
                result[0] = notificationResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(NotificationService.NotificationResult.SUCCESS, result[0]);
        assertNotNull(repo.sentNotification);
        assertNotNull(repo.loggedNotification);
        assertEquals("SELECTED", repo.sentNotification.getType());
        assertTrue(repo.sentNotification.getMessage().contains("complete your registration"));
        assertTrue(repo.sentNotification.getMessage().contains("Bring your ID"));
        assertEquals(repo.sentNotification.getMessage(), repo.loggedNotification.getMessage());
    }

    // helper method to create a sample event
    private Event makeSampleEvent() {
        long now = System.currentTimeMillis();
        return new Event(
                "event123",
                "Sample Event",
                "Sample Description",
                now - 1000000,
                now + 1000000
        );
    }
}
