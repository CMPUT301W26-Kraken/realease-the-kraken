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

    public WaitingListRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public WaitingListRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public interface BooleanCallback {
        void onResult(boolean value);
        void onError(Exception e);
    }

    public interface CompletionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // new used by EventDetailsFragment
    public interface CheckCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }

    public void isEntrantAlreadyWaiting(String eventId, String entrantId, BooleanCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(entrantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> callback.onResult(documentSnapshot.exists()))
                .addOnFailureListener(callback::onError);
    }

    // New method
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

    public void removeFromWaitingList(String eventId, String entrantId, CompletionCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(entrantId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // Ethan's code
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

    public interface EntrantsCallback {
        void onResult(ArrayList<String> entrants);
        void onError(Exception e);
    }

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