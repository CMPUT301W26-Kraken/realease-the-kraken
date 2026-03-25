package com.example.releasethekraken.model;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository responsible for saving and retrieving waiting list entries from Firestore.
 * This class belongs to the model layer and contains no UI logic.
 * It communicates directly with Firebase to manage waiting list data
 * associated with events.
 */
public class WaitingListRepository {

    /** Firestore database instance used for all operations */
    private final FirebaseFirestore db;

    /**
     * Creates a WaitingListRepository using the default Firestore instance.
     */
    public WaitingListRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Creates a WaitingListRepository with a specific Firestore instance.
     *
     * @param db the Firestore database instance to use
     */
    public WaitingListRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Callback interface for operations that return a boolean result.
     */
    public interface BooleanCallback {

        /**
         * Called when the operation completes successfully.
         *
         * @param value true if condition is met, false otherwise
         */
        void onResult(boolean value);

        /**
         * Called when an error occurs during the operation.
         *
         * @param e exception describing the failure
         */
        void onError(Exception e);
    }

    /**
     * Callback interface for operations that report completion status.
     */
    public interface CompletionCallback {

        /**
         * Called when the operation completes successfully.
         */
        void onSuccess();

        /**
         * Called when an error occurs during the operation.
         *
         * @param e exception describing the failure
         */
        void onError(Exception e);
    }

    /**
     * Callback interface used to check if a user exists in the waiting list.
     * Used specifically by EventDetailsFragment.
     */
    public interface CheckCallback {

        /**
         * Called when the check completes.
         *
         * @param exists true if the user is in the waiting list
         */
        void onResult(boolean exists);

        /**
         * Called when an error occurs.
         *
         * @param e exception describing the failure
         */
        void onError(Exception e);
    }

    /**
     * Callback interface used to return waiting list counts.
     */
    public interface CountCallback {
        void onResult(int count);
        void onError(Exception e);
    }

    /**
     * Checks if an entrant is already on the waiting list for a given event.
     *
     * @param eventId   the ID of the event
     * @param entrantId the ID of the entrant (user/device)
     * @param callback  callback returning true if entrant exists in waiting list
     */
    public void isEntrantAlreadyWaiting(String eventId, String entrantId, BooleanCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(entrantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> callback.onResult(documentSnapshot.exists()))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Checks if a user is currently in the waiting list for a given event.
     * This is functionally similar to isEntrantAlreadyWaiting but uses a dedicated callback.
     *
     * @param eventId   the ID of the event
     * @param entrantId the ID of the entrant
     * @param callback  callback returning whether the user exists in the waiting list
     */
    public void isUserInWaitingList(String eventId, String entrantId, CheckCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(entrantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    callback.onResult(documentSnapshot.exists());
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Counts the current number of entrants in an event waiting list.
     *
     * @param eventId the event to count entrants for
     * @param callback callback returning the current waiting list size
     */
    public void getWaitingListCount(String eventId, CountCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onResult(querySnapshot.size()))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Adds a waiting list entry to Firestore.
     *
     * @param entry    the waiting list entry to add
     * @param callback callback to indicate success or failure
     */
    public void addToWaitingList(WaitingListEntry entry, CompletionCallback callback) {
        DocumentReference docRef = db.collection("events")
                .document(entry.getEventId())
                .collection("waitingList")
                .document(entry.getEntrantId());

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", entry.getEventId());
        data.put("entrantId", entry.getEntrantId());
        data.put("joinedAtMillis", entry.getJoinedAtMillis());

        docRef.set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Removes an entrant from the waiting list for a given event.
     *
     * @param eventId   the ID of the event
     * @param entrantId the ID of the entrant
     * @param callback  callback to indicate success or failure
     */
    public void removeFromWaitingList(String eventId, String entrantId, CompletionCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(entrantId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Retrieves all entrant IDs currently on the waiting list for a given event.
     *
     * @param eventId the ID of the event
     * @param callback callback returning a list of entrant IDs
     */
    public void getAllEntrants(String eventId, EntrantsCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ArrayList<String> entrants = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        entrants.add(doc.getId());
                    }
                    callback.onResult(entrants);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface used to return a list of entrant IDs.
     */
    public interface EntrantsCallback {

        /**
         * Called when entrants are successfully retrieved.
         *
         * @param entrants list of entrant IDs
         */
        void onResult(List<String> entrants);

        /**
         * Called when an error occurs.
         *
         * @param e exception describing the failure
         */
        void onError(Exception e);
    }


    //Updated by chatGPT from original implementation "update this code based on changes to DrawEntrantsWorker" 2026-03-23
    /**
     * Saves the results of a lottery draw for an event.
     *
     * <p>Saves accepted entrants to the "accepted" subcollection and rejected
     * entrants to the "rejected" subcollection. Calls the provided callback
     * after all writes succeed or if any write fails.
     *
     * @param eventId   The ID of the event.
     * @param accepted  List of accepted entrants (winners).
     * @param rejected  List of rejected entrants.
     * @param callback  Callback to notify when the operation completes or fails.
     */
    public void saveDrawnEntrants(
            String eventId,
            List<String> accepted,
            List<String> rejected,
            CompletionCallback callback
    ) {
        int total = accepted.size() + rejected.size();

        // If there’s nothing to save, return immediately
        if (total == 0) {
            callback.onSuccess();
            return;
        }

        final int[] savedCount = {0};
        final boolean[] errorOccurred = {false};

        Runnable checkComplete = () -> {
            savedCount[0]++;
            if (savedCount[0] == total && !errorOccurred[0]) {
                callback.onSuccess();
            }
        };

        // Save accepted entrants
        for (String winnerId : accepted) {
            db.collection("events")
                    .document(eventId)
                    .collection("accepted")
                    .document(winnerId)
                    .set(Map.of("selected", true))
                    .addOnSuccessListener(unused -> checkComplete.run())
                    .addOnFailureListener(e -> {
                        if (!errorOccurred[0]) {
                            errorOccurred[0] = true;
                            callback.onError(e);
                        }
                    });
        }

        // Save rejected entrants
        for (String rejectedId : rejected) {
            db.collection("events")
                    .document(eventId)
                    .collection("rejected")
                    .document(rejectedId)
                    .set(Map.of("selected", false))
                    .addOnSuccessListener(unused -> checkComplete.run())
                    .addOnFailureListener(e -> {
                        if (!errorOccurred[0]) {
                            errorOccurred[0] = true;
                            callback.onError(e);
                        }
                    });
        }
    }
}
