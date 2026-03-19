package com.example.releasethekraken.model;

import com.google.firebase.firestore.FirebaseFirestore;
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
        // Write the full event shape to Firestore. Capacity is now persisted so browse-time
        // filtering can use real event data instead of a hardcoded value.
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("title", event.getTitle());
        data.put("description", event.getDescription());
        data.put("registrationStartMillis", event.getRegistrationStartMillis());
        data.put("registrationEndMillis", event.getRegistrationEndMillis());
        data.put("capacity", event.getCapacity());

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
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Treat a missing document as a repository error instead of returning a
                    // partially constructed Event to the UI layer.
                    if (!documentSnapshot.exists()) {
                        callback.onError(new Exception("Event not found"));
                        return;
                    }

                    // Read each field defensively because older documents may predate newer
                    // fields like capacity, and Firestore values may be null.
                    String title = documentSnapshot.getString("title");
                    String description = documentSnapshot.getString("description");
                    Long registrationStartMillis = documentSnapshot.getLong("registrationStartMillis");
                    Long registrationEndMillis = documentSnapshot.getLong("registrationEndMillis");
                    Long capacity = documentSnapshot.getLong("capacity");

                    // Normalize missing strings so adapter and search code can treat event data
                    // as non-null without adding repeated null checks.
                    if (title == null) {
                        title = "";
                    }
                    if (description == null) {
                        description = "";
                    }
                    // Normalize timestamps to zero when absent. This preserves backward
                    // compatibility and avoids crashing on incomplete seed data.
                    if (registrationStartMillis == null) {
                        registrationStartMillis = 0L;
                    }
                    if (registrationEndMillis == null) {
                        registrationEndMillis = 0L;
                    }
                    // Events created before capacity support should still load and remain
                    // filterable, so repository fallback matches Event.DEFAULT_CAPACITY.
                    if (capacity == null || capacity <= 0) {
                        capacity = (long) Event.DEFAULT_CAPACITY;
                    }

                    // Use the Firestore document id as the canonical id, since that is what the
                    // rest of the app navigates with when opening event details.
                    Event event = new Event(
                            documentSnapshot.getId(),
                            title,
                            description,
                            registrationStartMillis,
                            registrationEndMillis,
                            capacity.intValue()
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
                        // Apply the same defensive normalization for collection reads so browse
                        // and detail screens see consistent Event objects.
                        String title = document.getString("title");
                        String description = document.getString("description");
                        Long registrationStartMillis = document.getLong("registrationStartMillis");
                        Long registrationEndMillis = document.getLong("registrationEndMillis");
                        Long capacity = document.getLong("capacity");

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
                        if (capacity == null || capacity <= 0) {
                            capacity = (long) Event.DEFAULT_CAPACITY;
                        }

                        // Convert each Firestore document into a model object immediately so the
                        // view/controller code only works with Event instances from this point on.
                        Event event = new Event(
                                document.getId(),
                                title,
                                description,
                                registrationStartMillis,
                                registrationEndMillis,
                                capacity.intValue()
                        );

                        events.add(event);
                    }

                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onError);
    }
}
