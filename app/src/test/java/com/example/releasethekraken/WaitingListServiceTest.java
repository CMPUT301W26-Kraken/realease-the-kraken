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

        @Override
        public boolean isEntrantAlreadyWaiting(String eventId, String entrantId) {
            return alreadyWaiting;
        }

        @Override
        public void addToWaitingList(WaitingListEntry entry) {
            savedEntry = entry;
        }

        @Override
        public void removeFromWaitingList(String eventId, String entrantId) {
            removed = true;
        }
    }

    @Test
    public void joinWaitingList_invalidInput_returnsInvalidInput() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        WaitingListService service = new WaitingListService(repo);

        WaitingListService.JoinResult result = service.joinWaitingList(null, "device123");
        assertEquals(WaitingListService.JoinResult.INVALID_INPUT, result);

        WaitingListService.JoinResult result2 = service.joinWaitingList(makeOpenEvent(), "   ");
        assertEquals(WaitingListService.JoinResult.INVALID_INPUT, result2);
    }

    @Test
    public void joinWaitingList_registrationClosed_returnsRegistrationClosed() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        WaitingListService service = new WaitingListService(repo);

        long now = System.currentTimeMillis();
        Event closedEvent = new Event("event123", now - 200000, now - 100000); // ended in the past

        WaitingListService.JoinResult result = service.joinWaitingList(closedEvent, "device123");
        assertEquals(WaitingListService.JoinResult.REGISTRATION_CLOSED, result);
        assertNull(repo.savedEntry); // nothing saved
    }

    @Test
    public void joinWaitingList_duplicate_returnsDuplicateEntry() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        repo.alreadyWaiting = true;

        WaitingListService service = new WaitingListService(repo);

        WaitingListService.JoinResult result = service.joinWaitingList(makeOpenEvent(), "device123");
        assertEquals(WaitingListService.JoinResult.DUPLICATE_ENTRY, result);
        assertNull(repo.savedEntry); // nothing saved
    }

    @Test
    public void joinWaitingList_success_savesEntryAndReturnsSuccess() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        repo.alreadyWaiting = false;

        WaitingListService service = new WaitingListService(repo);

        Event event = makeOpenEvent();
        String entrantId = "device123";

        WaitingListService.JoinResult result = service.joinWaitingList(event, entrantId);
        assertEquals(WaitingListService.JoinResult.SUCCESS, result);

        // verify it saved something
        assertNotNull(repo.savedEntry);
        assertEquals(event.getEventId(), repo.savedEntry.getEventId());
        assertEquals(entrantId, repo.savedEntry.getEntrantId());
        assertTrue(repo.savedEntry.getJoinedAtMillis() > 0);    }

    // helper: registration window definitely open now
    private Event makeOpenEvent() {
        long now = System.currentTimeMillis();
        return new Event("event123", now - 1000000, now + 1000000);
    }

    @Test
    public void leaveWaitingList_invalidInput_returnsInvalidInput() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        WaitingListService service = new WaitingListService(repo);

        WaitingListService.LeaveResult result = service.leaveWaitingList(null, "device123");
        assertEquals(WaitingListService.LeaveResult.INVALID_INPUT, result);

        WaitingListService.LeaveResult result2 = service.leaveWaitingList(makeOpenEvent(), "   ");
        assertEquals(WaitingListService.LeaveResult.INVALID_INPUT, result2);
    }

    @Test
    public void leaveWaitingList_notOnWaitingList_returnsNotOnWaitingList() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        repo.alreadyWaiting = false;

        WaitingListService service = new WaitingListService(repo);

        WaitingListService.LeaveResult result = service.leaveWaitingList(makeOpenEvent(), "device123");
        assertEquals(WaitingListService.LeaveResult.NOT_ON_WAITING_LIST, result);
        assertFalse(repo.removed);
    }

    @Test
    public void leaveWaitingList_success_removesEntrant() {
        FakeWaitingListRepository repo = new FakeWaitingListRepository();
        repo.alreadyWaiting = true;

        WaitingListService service = new WaitingListService(repo);

        WaitingListService.LeaveResult result = service.leaveWaitingList(makeOpenEvent(), "device123");
        assertEquals(WaitingListService.LeaveResult.SUCCESS, result);
        assertTrue(repo.removed);
    }


}