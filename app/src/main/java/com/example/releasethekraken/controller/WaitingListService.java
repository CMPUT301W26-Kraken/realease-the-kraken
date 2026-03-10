package com.example.releasethekraken.controller;

//other classes this uses
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.model.WaitingListEntry;
import com.example.releasethekraken.model.Event;

//implements rules for the joining of the waiting list
//validate registration window
//prevent duplicates
//store in Firestore
//no UI

public class WaitingListService {

    //repo that stores and retrieves waiting list data entries from firestore
    private final WaitingListRepository waitingListRepository;

    //creates a WaitingListService with the repo dependency
    //this makes it easy to swap repos in the future (firestore)
    public WaitingListService(WaitingListRepository waitingListRepository) {
        this.waitingListRepository = waitingListRepository;
    }

    //possible outcomes of attempting to join the waiting list
    //used enum to make it simple for the View to show correct message,
    // can also add more outcomes if needed
    public enum JoinResult {
        SUCCESS,
        REGISTRATION_CLOSED,
        DUPLICATE_ENTRY,
        INVALID_INPUT,
    }

    //possible outcomes of attempting to leave the waiting list
    //used enum to make it simple for the View to show correct message,
    public enum LeaveResult {
        SUCCESS,
        NOT_ON_WAITING_LIST,
        INVALID_INPUT,
    }

    /**
     * leave the waiting list for an event US 01.01.02
     *  - Entrant can leave the waiting list
     *  - Entrant is removed from the waiting list
     *
     * @param event event the entrant is trying to leave
     * @param entrantId entrant/device identifier
     * @return LeaveResult indicating what happened
     */

    public LeaveResult leaveWaitingList(Event event, String entrantId) {

        //to avoid null or empty values causing crashes
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            return LeaveResult.INVALID_INPUT;
        }

        // check if entrant is actually on the waiting list
        boolean alreadyWaiting = waitingListRepository.isEntrantAlreadyWaiting(
                event.getEventId(),
                entrantId
        );

        if (!alreadyWaiting) {
            return LeaveResult.NOT_ON_WAITING_LIST;
        }

        // remove entrant from waiting list
        waitingListRepository.removeFromWaitingList(
                event.getEventId(),
                entrantId
        );

        return LeaveResult.SUCCESS;
    }

    /**
     * checks whether the current time is inside the events registration period
     *
     * @param event The event being joined
     * @param nowMillis Current time in milliseconds since epoch
     * @return true if registration is open, false otherwise
     */
    public boolean isRegistrationOpen(Event event, long nowMillis) {

        // registration is open if now is between start and end inclusive
        if (event == null) {
            return false;
        }

        return nowMillis >= event.getRegistrationStartMillis()
                && nowMillis <= event.getRegistrationEndMillis();
    }

    /**
     * join the waiting list for an event US 01.01.01
     * this method enforces acceptance criteria
     *  - Entrant can join during registration period
     *  - Duplicate entries prevented
     *  - Entry is stored via WaitingListRepository using Firestore
     *
     * @param event event the entrant is trying to join
     * @param entrantId  entrant/device identifier
     * @return JoinResult indicating what happened
     */
    public JoinResult joinWaitingList(Event event, String entrantId) {

        //validation to avoid null or empty values causing crashes
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            return JoinResult.INVALID_INPUT;
        }

        long nowMillis = System.currentTimeMillis();

        // 1. validate registration window
        if (!isRegistrationOpen(event, nowMillis)) {
            return JoinResult.REGISTRATION_CLOSED;
        }

        // 2. prevent duplicates
        // NOTE: This is synchronous right now because no Firestore
        // later with Firestore, this will likely become asynchronous tbd
        boolean alreadyWaiting = waitingListRepository.isEntrantAlreadyWaiting(
                event.getEventId(),
                entrantId
        );

        if (alreadyWaiting) {
            return JoinResult.DUPLICATE_ENTRY;
        }

        // 3. create the waiting list entry with exact join time
        WaitingListEntry entry = new WaitingListEntry(
                event.getEventId(),
                entrantId,
                nowMillis
        );

        // 4. store it repository will later store in Firestore
        waitingListRepository.addToWaitingList(entry);

        return JoinResult.SUCCESS;
    }
}