package com.example.releasethekraken.controller;

import com.example.releasethekraken.model.LotteryManager;
import com.example.releasethekraken.model.LotteryResult;
import com.example.releasethekraken.model.Notification;
import com.example.releasethekraken.model.NotificationRepository;
import com.example.releasethekraken.model.WaitingListRepository;
import com.example.releasethekraken.model.WaitingListEntry;
import com.example.releasethekraken.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        ALREADY_ORGANIZER,
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
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            if (callback != null) callback.onResult(LeaveResult.INVALID_INPUT);
            return;
        }

        waitingListRepository.isEntrantAlreadyWaiting(event.getEventId(), entrantId,
                new WaitingListRepository.BooleanCallback() {
                    @Override
                    public void onResult(boolean alreadyWaiting) {
                        if (!alreadyWaiting) {
                            if (callback != null) callback.onResult(LeaveResult.NOT_ON_WAITING_LIST);
                            return;
                        }

                        waitingListRepository.removeFromWaitingList(event.getEventId(), entrantId,
                                new WaitingListRepository.CompletionCallback() {
                                    @Override
                                    public void onSuccess() {
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        NotificationRepository notificationRepository = new NotificationRepository();

                                        // Remove from accepted if exists
                                        db.collection("events")
                                                .document(event.getEventId())
                                                .collection("accepted")
                                                .document(entrantId)
                                                .delete()
                                                .addOnSuccessListener(unused -> {
                                                    long nowMillis = System.currentTimeMillis();

                                                    // Create a cancelled notification record
                                                    Notification cancelledNotification = new Notification(
                                                            null,
                                                            entrantId,
                                                            event.getEventId(),
                                                            "", // optional: event title
                                                            "You have left the event",
                                                            "CANCELLED",
                                                            nowMillis,
                                                            false,
                                                            "cancelled"
                                                    );

                                                    // ALWAYS log to Firestore first
                                                    notificationRepository.logNotification(cancelledNotification, new NotificationRepository.CompletionCallback() {
                                                        @Override
                                                        public void onSuccess() {

                                                            // Optional: send push notification after logging
                                                            notificationRepository.sendNotification(cancelledNotification, null);

                                                            if (callback != null) callback.onResult(LeaveResult.SUCCESS);

                                                            if (!isRegistrationOpen(event, nowMillis)) {
                                                                triggerRedrawForAvailableSpots(event);
                                                            }
                                                        }

                                                        @Override
                                                        public void onError(Exception e) {
                                                            if (callback != null) callback.onError(e);
                                                        }
                                                    });

                                                })
                                                .addOnFailureListener(e -> {
                                                    if (callback != null) callback.onError(e);
                                                });
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        if (callback != null) callback.onError(e);
                                    }
                                });
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) callback.onError(e);
                    }
                });
    }

    public void handleWaitingListLeaveAndReplacement(
            Event event,
            String leavingEntrantId,
            LeaveCallback callback
    ) {
        leaveWaitingList(event, leavingEntrantId, new LeaveCallback() {
            @Override
            public void onResult(LeaveResult result) {

                if (result != LeaveResult.SUCCESS) {
                    if (callback != null) callback.onResult(result);
                    return;
                }

                long now = System.currentTimeMillis();

                // Only trigger replacement AFTER registration closes
                if (isRegistrationOpen(event, now)) {
                    if (callback != null) callback.onResult(LeaveResult.SUCCESS);
                    return;
                }

                // Trigger replacement selection
                triggerReplacementFromWaitingList(event.getEventId(), new WaitingListRepository.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        if (callback != null) callback.onResult(LeaveResult.SUCCESS);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    private void triggerReplacementFromWaitingList(
            String eventId,
            WaitingListRepository.CompletionCallback callback
    ) {
        waitingListRepository.getAllEntrants(eventId, new WaitingListRepository.EntrantsCallback() {
            @Override
            public void onResult(List<String> waitingListIds) {

                waitingListRepository.getAcceptedEntrants(eventId, new WaitingListRepository.EntrantsCallback() {
                    @Override
                    public void onResult(List<String> acceptedIds) {

                        List<String> candidates = new ArrayList<>();

                        for (String id : waitingListIds) {
                            if (!acceptedIds.contains(id)) {
                                candidates.add(id);
                            }
                        }

                        if (candidates.isEmpty()) {
                            callback.onSuccess();
                            return;
                        }

                        String selectedId = candidates.get(
                                new java.util.Random().nextInt(candidates.size())
                        );

                        saveReplacementAndNotify(eventId, selectedId, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            private void saveReplacementAndNotify(
                    String eventId,
                    String entrantId,
                    WaitingListRepository.CompletionCallback callback
            ) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                NotificationRepository notificationRepository = new NotificationRepository();

                long now = System.currentTimeMillis();

                Map<String, Object> acceptedData = new HashMap<>();
                acceptedData.put("selected", true);
                acceptedData.put("status", "pending");
                acceptedData.put("selectedAtMillis", now);

                db.collection("events")
                        .document(eventId)
                        .collection("accepted")
                        .document(entrantId)
                        .set(acceptedData)
                        .addOnSuccessListener(unused -> {

                            // Create notification
                            Notification notification = new Notification(
                                    null,
                                    entrantId,
                                    eventId,
                                    "", // optional: event title if you have it
                                    "A spot opened up! You’ve been selected.",
                                    "SELECTED",
                                    now,
                                    false,
                                    "pending"
                            );

                            notificationRepository.sendNotification(notification, new NotificationRepository.CompletionCallback() {
                                @Override
                                public void onSuccess() {
                                    notificationRepository.logNotification(notification, null);
                                    callback.onSuccess();
                                }

                                @Override
                                public void onError(Exception e) {
                                    callback.onError(e);
                                }
                            });
                        })
                        .addOnFailureListener(callback::onError);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void triggerRedrawForAvailableSpots(Event event) {
        // 1. Get current accepted count
        waitingListRepository.getAcceptedEntrants(event.getEventId(),
                new WaitingListRepository.EntrantsCallback() {
                    @Override
                    public void onResult(List<String> acceptedIds) {

                        int acceptedCount = acceptedIds.size();
                        int capacity = event.getCapacity();

                        int spotsAvailable = capacity - acceptedCount;
                        if (spotsAvailable <= 0) {
                            // No need to redraw
                            return;
                        }

                        // 2. Get current waiting list
                        waitingListRepository.getAllEntrants(event.getEventId(),
                                new WaitingListRepository.EntrantsCallback() {
                                    @Override
                                    public void onResult(List<String> waitingListIds) {
                                        // 3. Filter out already accepted/rejected entrants
                                        waitingListIds.removeAll(acceptedIds);

                                        if (waitingListIds.isEmpty()) return;

                                        // 4. Draw winners for available spots
                                        LotteryResult result = new LotteryManager()
                                                .drawEntrants(event, waitingListIds, spotsAvailable);

                                        List<String> newAccepted = result.accepted;
                                        List<String> newlyRejected = result.rejected;

                                        // 5. Save results without touching existing accepted
                                        waitingListRepository.saveDrawnEntrants(
                                                event.getEventId(),
                                                newAccepted,
                                                newlyRejected,
                                                new WaitingListRepository.CompletionCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        System.out.println("Redraw successful for open spots!");
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

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });
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
     * join the waiting list for an event without location data
     * delegates to the full method with 0.0 coordinates
     *
     * @param event event the entrant is trying to join
     * @param entrantId  entrant/device identifier
     * @return JoinResult indicating what happened
     * @param entrantId entrant/device identifier
     */
    public void joinWaitingList(Event event, String entrantId, JoinCallback callback) {
        joinWaitingList(event, entrantId, 0.0, 0.0, callback);
    }

    /**
     * join the waiting list for an event with optional location data
     * this method enforces acceptance criteria
     *  - Entrant can join during registration period
     *  - Duplicate entries prevented
     *  - Entry is stored via WaitingListRepository using Firestore
     *
     * @param event     event the entrant is trying to join
     * @param entrantId entrant/device identifier
     * @param latitude  latitude where entrant joined, 0.0 if not captured
     * @param longitude longitude where entrant joined, 0.0 if not captured
     * @param callback  callback returning the join result
     */
    public void joinWaitingList(Event event, String entrantId,
                                double latitude, double longitude, JoinCallback callback) {
        //validation to avoid null or empty values causing crashes
        if (event == null || entrantId == null || entrantId.trim().isEmpty()) {
            if (callback != null) callback.onResult(JoinResult.INVALID_INPUT);
            return;
        }

        // Prevent organizers/co-organizers from joining the waiting list
        if (entrantId.equals(event.getOrganizerId()) || event.getCoOrganizerIds().contains(entrantId)) {
            if (callback != null) callback.onResult(JoinResult.ALREADY_ORGANIZER);
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
                        // 3. create the waiting list entry with exact join time and coordinates
                        WaitingListEntry entry = new WaitingListEntry(
                                event.getEventId(),
                                entrantId,
                                nowMillis,
                                latitude,
                                longitude
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