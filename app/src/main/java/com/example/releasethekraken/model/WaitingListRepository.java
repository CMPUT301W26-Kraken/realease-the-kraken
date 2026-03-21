package com.example.releasethekraken.model;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
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
        void onResult(ArrayList<String> entrants);

        /**
         * Called when an error occurs.
         *
         * @param e exception describing the failure
         */
        void onError(Exception e);
    }

    /**
     * Saves accepted entrants (winners) for an event into Firestore.
     * Each winner is stored under a separate document in the "accepted" collection.
     *
     * @param eventId the ID of the event
     * @param winners list of entrant IDs selected as winners
     * @param callback callback to indicate when all writes succeed or if an error occurs
     */
    public void saveAcceptedEntrants(String eventId, ArrayList<String> winners, CompletionCallback callback) {
        int total = winners.size();
        final int[] savedCount = {0};
        final boolean[] errorOccurred = {false};

        for (String winnerId : winners) {
            db.collection("events")
                    .document(eventId)
                    .collection("accepted")
                    .document(winnerId)
                    .set(Map.of("selected", true))
                    .addOnSuccessListener(unused -> {
                        savedCount[0]++;
                        if (savedCount[0] == total && !errorOccurred[0]) {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!errorOccurred[0]) {
                            errorOccurred[0] = true;
                            callback.onError(e);
                        }
                    });
        }
    }
}