package com.example.releasethekraken.model;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * repository responsible for saving and retrieving waiting list entries from Firestore
 * this class belongs to the model layer and contains no UI logic
 * it communicates with Firebase to manage waiting list data for events
 */
public class WaitingListRepository {
    private final FirebaseFirestore db;
    /**
     * creates a WaitingListRepository using the default Firestore instance
     */
    public WaitingListRepository() {
        this(FirebaseFirestore.getInstance());
    }
    /**
     * creates a WaitingListRepository with a specific Firestore instance
     *
     * @param db the Firestore database instance to use
     */
    public WaitingListRepository(FirebaseFirestore db) {
        this.db = db;
    }
    /**
     * callback interface for operations that return a boolean result
     */
    public interface BooleanCallback {
        void onResult(boolean value);
        void onError(Exception e);
    }
    /**
     * Callback interface for operations that report completion status
     */
    public interface CompletionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    /**
     * checks if an entrant is already on the waiting list for an event
     * @param eventId event ID
     * @param entrantId entrant ID
     * @param callback callback returning true if entrant exists
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
     * adds a waiting list entry to Firestore
     * @param entry waiting list entry to add
     * @param callback success/error callback
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
     * removes an entrant from the waiting list for an event
     * @param eventId event ID
     * @param entrantId entrant ID
     * @param callback success/error callback
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

    //Ethan's cool added code for getting all entrants from an event
    /**
     * Get all the entrant ids for a given event
     * @param eventId id of the event
     * @param callback returns list of entrant ids
     */
    public void getAllEntrants(String eventId, EntrantsCallback callback) {
        db.collection("events") //Go through our database and find our waitingList for this event
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    //If we find waht we are looking for, lets go through it and make it into a arraylist, YAY!
                    ArrayList<String> entrants = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        entrants.add(doc.getId());
                    }
                    callback.onResult(entrants);
                })
                .addOnFailureListener(callback::onError);
    }

    /** Callback interface to return a list of entrants */
    public interface EntrantsCallback {
        void onResult(ArrayList<String> entrants);
        void onError(Exception e);
    }

    /**
     * Save accepted entrants for an event
     * @param eventId id of the event
     * @param winners list of accepted entrant ids
     * @param callback success/error callback
     */
    public void saveAcceptedEntrants(String eventId, ArrayList<String> winners, CompletionCallback callback) {
        int total = winners.size();
        final int[] savedCount = {0}; //gotta use these arrays so that they can be used in lambda
        final boolean[] errorOccurred = {false};

        for (String winnerId : winners) { //iterate through all the winners and add them to the firebase
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
