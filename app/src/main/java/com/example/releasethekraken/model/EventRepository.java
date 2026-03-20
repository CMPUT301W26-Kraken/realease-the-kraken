package com.example.releasethekraken.model;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * repository class responsible for creating and retrieving Event objects
 * from Firestore
 */
public class EventRepository {

    private final FirebaseFirestore db;
    /**
     * creates an EventRepository using the default Firestore instance
     */
    public EventRepository() {
        this(FirebaseFirestore.getInstance());
    }
    /**
     * creates an EventRepository with a specific Firestore instance
     *
     * @param db the Firestore database instance to use
     */
    public EventRepository(FirebaseFirestore db) {
        this.db = db;
    }
    /**
     * callback interface for returning a single Event result
     */
    public interface EventCallback {
        void onSuccess(Event event);
        void onError(Exception e);
    }
    /**
     * callback interface for returning a list of Event objects
     */
    public interface EventsCallback {
        void onSuccess(List<Event> events);
        void onError(Exception e);
    }
    /**
     * callback interface for operations that  report completion status
     */
    public interface CompletionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    /**
     * creates a new event in Firestore
     *
     * @param event the event to save
     * @param callback callback used to report success or failure
     */
    public void createEvent(Event event, CompletionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("title", event.getTitle());
        data.put("description", event.getDescription());
        data.put("registrationStartMillis", event.getRegistrationStartMillis());
        data.put("registrationEndMillis", event.getRegistrationEndMillis());
        data.put("createdAt", System.currentTimeMillis()); // Added for sorting

        db.collection("events")
                .document(event.getEventId())
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * gets a single event from Firestore by its document ID
     *
     * @param eventId the ID of the event document to retrieve
     * @param callback callback used to return the event or an error
     */
    public void getEventById(String eventId, EventCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new Exception("Invalid event ID"));
            return;
        }

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onError(new Exception("Event not found"));
                        return;
                    }

                    String title = documentSnapshot.getString("title");
                    String description = documentSnapshot.getString("description");
                    Long registrationStartMillis = documentSnapshot.getLong("registrationStartMillis");
                    Long registrationEndMillis = documentSnapshot.getLong("registrationEndMillis");

                    if (title == null) {
                        title = "";
                    }
                    if (description == null) {
                        description = "";
                    }
                    if (registrationStartMillis == null) {
                        registrationStartMillis = 0L;
                    }
                    if (registrationEndMillis == null) {
                        registrationEndMillis = 0L;
                    }

                    Event event = new Event(
                            documentSnapshot.getId(),
                            title,
                            description,
                            registrationStartMillis,
                            registrationEndMillis
                    );

                    callback.onSuccess(event);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * gets all events from Firestore
     *
     * @param callback callback used to return the list of events or an error
     */
    public void getAllEvents(EventsCallback callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String title = document.getString("title");
                        String description = document.getString("description");
                        Long registrationStartMillis = document.getLong("registrationStartMillis");
                        Long registrationEndMillis = document.getLong("registrationEndMillis");

                        if (title == null) {
                            title = "";
                        }
                        if (description == null) {
                            description = "";
                        }
                        if (registrationStartMillis == null) {
                            registrationStartMillis = 0L;
                        }
                        if (registrationEndMillis == null) {
                            registrationEndMillis = 0L;
                        }

                        Event event = new Event(
                                document.getId(),
                                title,
                                description,
                                registrationStartMillis,
                                registrationEndMillis
                        );

                        events.add(event);
                    }

                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * gets the most recently created event from Firestore
     *
     * @param callback callback used to return the event or an error
     */
    public void getMostRecentEvent(EventCallback callback) {
        db.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onError(new Exception("No events found"));
                        return;
                    }
                    
                    QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                    String title = document.getString("title");
                    String description = document.getString("description");
                    Long registrationStartMillis = document.getLong("registrationStartMillis");
                    Long registrationEndMillis = document.getLong("registrationEndMillis");

                    Event event = new Event(
                            document.getId(),
                            title != null ? title : "",
                            description != null ? description : "",
                            registrationStartMillis != null ? registrationStartMillis : 0L,
                            registrationEndMillis != null ? registrationEndMillis : 0L
                    );
                    callback.onSuccess(event);
                })
                .addOnFailureListener(callback::onError);
    }
}
