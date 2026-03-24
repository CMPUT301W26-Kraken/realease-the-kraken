package com.example.releasethekraken.model;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

    // --- Added for event image upload ---
    // Root reference to Firebase Storage bucket — used by uploadEventPoster()
    private final StorageReference storageRef =
            FirebaseStorage.getInstance().getReference();
    // --- End event image additions ---

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
     * callback interface for operations that report completion status
     */
    public interface CompletionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // --- Added for event image upload ---
    /**
     * Callback interface for operations that return a String result.
     * Used by uploadEventPoster() to return the HTTPS download URL back to the caller.
     * The existing CompletionCallback only signals success/failure with no return value.
     */
    public interface CompletionCallback2 {
        void onSuccess(String result); // result = the HTTPS download URL from Storage
        void onError(Exception e);
    }
    // --- End event image additions ---

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
                    Long capacity = document.getLong("capacity");

                    if (capacity == null || capacity <= 0) {
                        capacity = (long) Event.DEFAULT_CAPACITY;
                    }

                    Event event = new Event(
                            document.getId(),
                            title != null ? title : "",
                            description != null ? description : "",
                            registrationStartMillis != null ? registrationStartMillis : 0L,
                            registrationEndMillis != null ? registrationEndMillis : 0L,
                            capacity.intValue()
                    );
                    callback.onSuccess(event);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Uploads an event poster image to Firebase Storage.
     * Storage path: event_posters/{eventId}.jpg
     * Using eventId as the filename means re-uploading overwrites the old poster.
     * After getting the URL back, call savePosterImageUrl() to save it to Firestore.
     *
     * @param eventId   the Firestore document ID of the event
     * @param imageUri  local URI of the image the organizer picked from gallery
     * @param callback  called with the HTTPS download URL on success
     */
    public void uploadEventPoster(String eventId, Uri imageUri, CompletionCallback2 callback) {
        // event_posters/{eventId}.jpg — overwrites on re-upload, no duplicate files
        StorageReference imageRef = storageRef.child("event_posters/" + eventId + ".jpg");

        // putFile() streams the local file up to Firebase Storage
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // upload succeeded — get the permanent HTTPS download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        Log.d("EventRepository", "Poster uploaded: " + downloadUri);
                        callback.onSuccess(downloadUri.toString()); // pass URL back to caller
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("EventRepository", "Poster upload failed", e);
                    callback.onError(e);
                });
    }

    /**
     * Saves the poster image URL to the existing Firestore event document.
     * Uses update() so only the posterImageUrl field changes — all other fields untouched.
     *
     * @param eventId   the Firestore document ID of the event
     * @param imageUrl  the HTTPS download URL returned by uploadEventPoster()
     * @param callback  called on success or failure
     */
    public void savePosterImageUrl(String eventId, String imageUrl, CompletionCallback callback) {
        db.collection("events")
                .document(eventId)
                .update("posterImageUrl", imageUrl) // update() only touches this one field
                .addOnSuccessListener(unused -> {
                    Log.d("EventRepository", "Poster URL saved to Firestore");
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onError);
    }
}