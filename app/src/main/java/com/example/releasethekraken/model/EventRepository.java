package com.example.releasethekraken.model;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for loading events from Firestore.
 */
public class EventRepository {

    private final FirebaseFirestore db;

    public EventRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public EventRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public interface EventCallback {
        void onSuccess(Event event);
        void onError(Exception e);
    }

    public interface EventsCallback {
        void onSuccess(List<Event> events);
        void onError(Exception e);
    }

    //gets one event by its Firestore document ID
    public void getEventById(String eventId, EventCallback callback) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onError(new Exception("Event not found"));
                        return;
                    }

                    Long registrationStartMillis = documentSnapshot.getLong("registrationStartMillis");
                    Long registrationEndMillis = documentSnapshot.getLong("registrationEndMillis");

                    if (registrationStartMillis == null) {
                        registrationStartMillis = 0L;
                    }
                    if (registrationEndMillis == null) {
                        registrationEndMillis = 0L;
                    }

                    Event event = new Event(
                            documentSnapshot.getId(),
                            registrationStartMillis,
                            registrationEndMillis
                    );

                    callback.onSuccess(event);
                })
                .addOnFailureListener(callback::onError);
    }

    //gets all events from Firestore

    public void getAllEvents(EventsCallback callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Long registrationStartMillis = document.getLong("registrationStartMillis");
                        Long registrationEndMillis = document.getLong("registrationEndMillis");

                        if (registrationStartMillis == null) {
                            registrationStartMillis = 0L;
                        }
                        if (registrationEndMillis == null) {
                            registrationEndMillis = 0L;
                        }

                        Event event = new Event(
                                document.getId(),
                                registrationStartMillis,
                                registrationEndMillis
                        );

                        events.add(event);
                    }

                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onError);
    }
}