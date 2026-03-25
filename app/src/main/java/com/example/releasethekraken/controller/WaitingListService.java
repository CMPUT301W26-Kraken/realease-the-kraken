package com.example.releasethekraken.controller;

import com.example.releasethekraken.model.LotteryManager;
import com.example.releasethekraken.model.LotteryResult;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.model.WaitingListEntry;
import com.example.releasethekraken.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * service class responsible for handling waiting list logic
 * this class validates registration windows, prevents duplicate entries,
 * and delegates waiting list data storage to the WaitingListRepository
 * it belongs to the controller layer and contains no UI logic
 */
public class WaitingListService {

    /** repository used to store and retrieve waiting list entries. */
    private final WaitingListRepository waitingListRepository;

    /**
     * creates a WaitingListService with the given repository dependency
     * @param waitingListRepository the repository used for waiting list operations
     */
    public WaitingListService(WaitingListRepository waitingListRepository) {
        this.waitingListRepository = waitingListRepository;
    }

    /**
     * possible outcomes when attempting to join the waiting list
     */
    public enum JoinResult {
        SUCCESS,
        REGISTRATION_CLOSED,
        DUPLICATE_ENTRY,
        WAITING_LIST_FULL,
        INVALID_INPUT,
    }

    /**
     * possible outcomes when attempting to leave the waiting list
     */
    public enum LeaveResult {
        SUCCESS,
        NOT_ON_WAITING_LIST,
        INVALID_INPUT,
    }

    /**
     * callback interface for join waiting list results
     */
    public interface JoinCallback {
        void onResult(JoinResult result);
        void onError(Exception e);
    }

    /**
     * callback interface for leave waiting list results
     */
    public interface LeaveCallback {
        void onResult(LeaveResult result);
        void onError(Exception e);
    }

    /**
     * leave the waiting list for an event
     *  - Entrant can leave the waiting list
     *  - Entrant is removed from the waiting list
     *
     * @param event event the entrant is trying to leave
     * @param entrantId entrant/device identifier
     */
    public void leaveWaitingList(Event event, String entrantId, LeaveCallback callback) {
        //to avoid null or empty values causing crashes
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            if (callback != null) callback.onResult(LeaveResult.INVALID_INPUT);
            return;
        }
        // check if entrant is actually on the waiting list
        waitingListRepository.isEntrantAlreadyWaiting(
                event.getEventId(),
                entrantId,
                new WaitingListRepository.BooleanCallback() {
                    @Override
                    public void onResult(boolean alreadyWaiting) {
                        if (!alreadyWaiting) {
                            if (callback != null) callback.onResult(LeaveResult.NOT_ON_WAITING_LIST);
                            return;
                        }

                        waitingListRepository.removeFromWaitingList(
                                event.getEventId(),
                                entrantId,
                                new WaitingListRepository.CompletionCallback() {
                                    @Override
                                    public void onSuccess() {
                                        if (callback != null) callback.onResult(LeaveResult.SUCCESS);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        if (callback != null) callback.onError(e);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) callback.onError(e);
                    }
                }
        );
    }

    /**
     * checks whether the current time is inside the events registration period
     *
     * @param event The event being joined
     * @param nowMillis Current time in milliseconds since epoch
     * @return true if registration is open, false otherwise
     */
    public boolean isRegistrationOpen(Event event, long nowMillis) {

        if (event == null) {
            return false;
        }

        return nowMillis >= event.getRegistrationStartMillis()
                && nowMillis <= event.getRegistrationEndMillis();
    }

    /**
     * join the waiting list for an event
     * this method enforces acceptance criteria
     *  - Entrant can join during registration period
     *  - Duplicate entries prevented
     *  - Entry is stored via WaitingListRepository using Firestore
     *
     * @param event event the entrant is trying to join
     * @param entrantId  entrant/device identifier
     * @return JoinResult indicating what happened
     * @param entrantId entrant/device identifier
     */
    public void joinWaitingList(Event event, String entrantId, JoinCallback callback) {
        //validation to avoid null or empty values causing crashes
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            if (callback != null) callback.onResult(JoinResult.INVALID_INPUT);
            return;
        }

        long nowMillis = System.currentTimeMillis();
        // 1. validate registration window
        if (!isRegistrationOpen(event, nowMillis)) {
            if (callback != null) callback.onResult(JoinResult.REGISTRATION_CLOSED);
            return;
        }
        // 2. prevent duplicates
        waitingListRepository.isEntrantAlreadyWaiting(
                event.getEventId(),
                entrantId,
                new WaitingListRepository.BooleanCallback() {
                    @Override
                    public void onResult(boolean alreadyWaiting) {
                        if (alreadyWaiting) {
                            if (callback != null) callback.onResult(JoinResult.DUPLICATE_ENTRY);
                            return;
                        }
                        // 3. enforce the organizer-configured maximum waiting list size before
                        // writing a new entrant to Firestore.
                        waitingListRepository.getWaitingListCount(
                                event.getEventId(),
                                new WaitingListRepository.CountCallback() {
                                    @Override
                                    public void onResult(int count) {
                                        if (event.getCapacity() > 0 && count >= event.getCapacity()) {
                                            if (callback != null) callback.onResult(JoinResult.WAITING_LIST_FULL);
                                            return;
                                        }

                                        // 4. create the waiting list entry with exact join time
                                        WaitingListEntry entry = new WaitingListEntry(
                                                event.getEventId(),
                                                entrantId,
                                                nowMillis
                                        );

                                        // 5. store it; the repository later persists it in Firestore
                                        waitingListRepository.addToWaitingList(
                                                entry,
                                                new WaitingListRepository.CompletionCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        if (callback != null) callback.onResult(JoinResult.SUCCESS);
                                                    }

                                                    @Override
                                                    public void onError(Exception e) {
                                                        if (callback != null) callback.onError(e);
                                                    }
                                                }
                                        );
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        if (callback != null) callback.onError(e);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) callback.onError(e);
                    }
                }
        );
    }

    //Updated from original implementaion by ChatGPT "Update this method to work with changes made" 2026-03-23
    /**
     * Draws winners for the given event.
     *
     * <p>Retrieves all entrants from Firebase, randomly selects winners based
     * on the event's capacity, separates entrants into accepted and rejected
     * groups, and saves both groups back to Firebase.
     *
     * @param event    The event to draw from
     * @param capacity The maximum number of winners to select
     */
    public void drawEntrants(Event event, int capacity) {

        waitingListRepository.getAllEntrants(event.getEventId(), new WaitingListRepository.EntrantsCallback() {
            @Override
            public void onResult(List<String> entrants) {

                // Run the lottery
                LotteryResult result = new LotteryManager().drawEntrants(event, entrants, capacity);

                List<String> winners = result.accepted;
                List<String> rejected = result.rejected;

                // Save both accepted and rejected entrants in Firebase
                waitingListRepository.saveDrawnEntrants(
                        event.getEventId(),
                        winners,
                        rejected,
                        new WaitingListRepository.CompletionCallback() {
                            @Override
                            public void onSuccess() {
                                System.out.println("Draw results saved successfully!");
                            }

                            @Override
                            public void onError(Exception e) {
                                e.printStackTrace();
                            }
                        }
                );
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
