package com.example.releasethekraken;

import com.example.releasethekraken.controller.WaitingListService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.WaitingListEntry;
import com.example.releasethekraken.model.WaitingListRepository;

import org.junit.Test;

import static org.junit.Assert.*;

public class WaitingListServiceTest {

    //fake repo so we can control behavior and inspect what gets saved
    static class FakeWaitingListRepository extends WaitingListRepository {
        boolean alreadyWaiting = false;
        boolean removed = false;
        WaitingListEntry savedEntry = null;

        public FakeWaitingListRepository() {
            super(null);
        }

        @Override
        public void isEntrantAlreadyWaiting(String eventId, String entrantId, BooleanCallback callback) {
            callback.onResult(alreadyWaiting);
        }

        @Override
        public void addToWaitingList(WaitingListEntry entry, CompletionCallback callback) {
            savedEntry = entry;
            callback.onSuccess();
        }

        @Override
        public void removeFromWaitingList(String eventId, String entrantId, CompletionCallback callback) {
            removed = true;
            callback.onSuccess();
        }
    }

    @Test
    public void joinWaitingList_invalidInput_returnsInvalidInput() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        WaitingListService service = new WaitingListService(repo);

        final WaitingListService.JoinResult[] result = new WaitingListService.JoinResult[1];

        service.joinWaitingList(null, "device123", new WaitingListService.JoinCallback() {
            @Override
            public void onResult(WaitingListService.JoinResult joinResult) {
                result[0] = joinResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.JoinResult.INVALID_INPUT, result[0]);

        final WaitingListService.JoinResult[] result2 = new WaitingListService.JoinResult[1];

        service.joinWaitingList(makeOpenEvent(), "   ", new WaitingListService.JoinCallback() {
            @Override
            public void onResult(WaitingListService.JoinResult joinResult) {
                result2[0] = joinResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.JoinResult.INVALID_INPUT, result2[0]);
    }

    @Test
    public void joinWaitingList_registrationClosed_returnsRegistrationClosed() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        WaitingListService service = new WaitingListService(repo);

        long now = System.currentTimeMillis();
        Event closedEvent = new Event("event123", now - 200000, now - 100000); // ended in the past

        final WaitingListService.JoinResult[] result = new WaitingListService.JoinResult[1];

        service.joinWaitingList(closedEvent, "device123", new WaitingListService.JoinCallback() {
            @Override
            public void onResult(WaitingListService.JoinResult joinResult) {
                result[0] = joinResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.JoinResult.REGISTRATION_CLOSED, result[0]);
        assertNull(repo.savedEntry); // nothing saved
    }

    @Test
    public void joinWaitingList_duplicate_returnsDuplicateEntry() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        repo.alreadyWaiting = true;

        WaitingListService service = new WaitingListService(repo);

        final WaitingListService.JoinResult[] result = new WaitingListService.JoinResult[1];

        service.joinWaitingList(makeOpenEvent(), "device123", new WaitingListService.JoinCallback() {
            @Override
            public void onResult(WaitingListService.JoinResult joinResult) {
                result[0] = joinResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.JoinResult.DUPLICATE_ENTRY, result[0]);
        assertNull(repo.savedEntry); // nothing saved
    }

    @Test
    public void joinWaitingList_success_savesEntryAndReturnsSuccess() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        repo.alreadyWaiting = false;

        WaitingListService service = new WaitingListService(repo);

        Event event = makeOpenEvent();
        String entrantId = "device123";

        final WaitingListService.JoinResult[] result = new WaitingListService.JoinResult[1];

        service.joinWaitingList(event, entrantId, new WaitingListService.JoinCallback() {
            @Override
            public void onResult(WaitingListService.JoinResult joinResult) {
                result[0] = joinResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.JoinResult.SUCCESS, result[0]);

        // verify it saved something
        assertNotNull(repo.savedEntry);
        assertEquals(event.getEventId(), repo.savedEntry.getEventId());
        assertEquals(entrantId, repo.savedEntry.getEntrantId());
        assertTrue(repo.savedEntry.getJoinedAtMillis() > 0);
    }

    @Test
    public void leaveWaitingList_invalidInput_returnsInvalidInput() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        WaitingListService service = new WaitingListService(repo);

        final WaitingListService.LeaveResult[] result = new WaitingListService.LeaveResult[1];

        service.leaveWaitingList(null, "device123", new WaitingListService.LeaveCallback() {
            @Override
            public void onResult(WaitingListService.LeaveResult leaveResult) {
                result[0] = leaveResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.LeaveResult.INVALID_INPUT, result[0]);

        final WaitingListService.LeaveResult[] result2 = new WaitingListService.LeaveResult[1];

        service.leaveWaitingList(makeOpenEvent(), "   ", new WaitingListService.LeaveCallback() {
            @Override
            public void onResult(WaitingListService.LeaveResult leaveResult) {
                result2[0] = leaveResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.LeaveResult.INVALID_INPUT, result2[0]);
    }

    @Test
    public void leaveWaitingList_notOnWaitingList_returnsNotOnWaitingList() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        repo.alreadyWaiting = false;

        WaitingListService service = new WaitingListService(repo);

        final WaitingListService.LeaveResult[] result = new WaitingListService.LeaveResult[1];

        service.leaveWaitingList(makeOpenEvent(), "device123", new WaitingListService.LeaveCallback() {
            @Override
            public void onResult(WaitingListService.LeaveResult leaveResult) {
                result[0] = leaveResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.LeaveResult.NOT_ON_WAITING_LIST, result[0]);
        assertFalse(repo.removed);
    }

    @Test
    public void leaveWaitingList_success_removesEntrant() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        repo.alreadyWaiting = true;

        WaitingListService service = new WaitingListService(repo);

        final WaitingListService.LeaveResult[] result = new WaitingListService.LeaveResult[1];

        service.leaveWaitingList(makeOpenEvent(), "device123", new WaitingListService.LeaveCallback() {
            @Override
            public void onResult(WaitingListService.LeaveResult leaveResult) {
                result[0] = leaveResult;
            }

            @Override
            public void onError(Exception e) {
                fail("Unexpected error: " + e.getMessage());
            }
        });

        assertEquals(WaitingListService.LeaveResult.SUCCESS, result[0]);
        assertTrue(repo.removed);
    }

    // helper: registration window definitely open now
    private Event makeOpenEvent() {
        long now = System.currentTimeMillis();
        return new Event("event123", now - 1000000, now + 1000000);
    }
}
