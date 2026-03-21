package com.example.releasethekraken.controller;

import com.example.releasethekraken.model.LotteryManager;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.model.WaitingListEntry;
import com.example.releasethekraken.model.Event;

import java.util.ArrayList;

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
                        // 3. create the waiting list entry with exact join time
                        WaitingListEntry entry = new WaitingListEntry(
                                event.getEventId(),
                                entrantId,
                                nowMillis
                        );
                        // 4. store it repository will later store in Firestore
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

    /**
     * Draws winners for the given event.
     * Retrieves all entrants from Firebase and randomly selects winners
     * based on the event's capacity, and saves the accepted winners
     * back to Firebase.
     * @param event    The event to be drawn from
     * @param capacity The maximum number of winners to select
     */
    public void drawEntrants(Event event, int capacity) {

        waitingListRepository.getAllEntrants(event.getEventId(), new WaitingListRepository.EntrantsCallback() {
            @Override
            public void onResult(ArrayList<String> entrants) {
                //Lottery!
                ArrayList<String> winners = new LotteryManager().drawEntrants(event, entrants, capacity);
                //save the winners in firebase! (got to do callback for firebase)
                waitingListRepository.saveAcceptedEntrants(event.getEventId(), winners, new WaitingListRepository.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        System.out.println("Winners saved successfully!");
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}