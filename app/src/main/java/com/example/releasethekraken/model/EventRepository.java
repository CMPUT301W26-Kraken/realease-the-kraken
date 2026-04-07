package com.example.releasethekraken.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

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

    public interface CompletionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public void createEvent(Event event, CompletionCallback callback) {
        createEvent(event, null, callback);
    }

    public void createEvent(Event event, String posterImageUrl, CompletionCallback callback) {
        String finalPosterUrl = posterImageUrl != null ? posterImageUrl : event.getPosterUrl();

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("title", event.getTitle());
        data.put("description", event.getDescription());
        data.put("registrationStartMillis", event.getRegistrationStartMillis());
        data.put("registrationEndMillis", event.getRegistrationEndMillis());
        data.put("capacity", event.getCapacity());
        data.put("isPrivate", event.isPrivate());
        data.put("invitedUserIds", event.getInvitedUserIds());
        data.put("coOrganizerIds", event.getCoOrganizerIds());
        data.put("organizerId", event.getOrganizerId());
        data.put("createdAt", System.currentTimeMillis());
        data.put("posterImageUrl", finalPosterUrl);
        data.put("posterUrl", finalPosterUrl);
        data.put("geolocationRequired", event.isGeolocationRequired());

        db.collection("events")
                .document(event.getEventId())
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Deletes an event from the Firebase database and also deletes its corresponding poster image
     * from the image storage
     *
     * @param event The event that we are deleting
     * @param posterImageUrl The url of the poster that is being deleted
     * @param callback The callback being used to notify the caller of success or failure
     */
    public void deleteEvent(Event event, String posterImageUrl, CompletionCallback callback) {
        String finalPosterUrl = posterImageUrl != null ? posterImageUrl : event.getPosterUrl();

        db.collection("events")
                .document(event.getEventId())
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);

        if (finalPosterUrl != null && !finalPosterUrl.isEmpty()) {
            FirebaseStorage.getInstance()
                    .getReferenceFromUrl(finalPosterUrl)
                    .delete();

        }
    }

    /**
     * Updates or inserts an event into Firestore.
     * @param event The event to save.
     * @param callback Callback for success or error.
     */
    public void upsertEvent(Event event, EventCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("title", event.getTitle());
        data.put("description", event.getDescription());
        data.put("registrationStartMillis", event.getRegistrationStartMillis());
        data.put("registrationEndMillis", event.getRegistrationEndMillis());
        data.put("capacity", event.getCapacity());
        data.put("isPrivate", event.isPrivate());
        data.put("invitedUserIds", event.getInvitedUserIds());
        data.put("coOrganizerIds", event.getCoOrganizerIds());
        data.put("organizerId", event.getOrganizerId());
        data.put("posterImageUrl", event.getPosterUrl());
        data.put("posterUrl", event.getPosterUrl());
        data.put("geolocationRequired", event.isGeolocationRequired());

        // Use set with merge to avoid overwriting createdAt if we just want to update
        // However, if it's a new event, we might want createdAt. 
        // For simplicity and matching the existing createEvent pattern:
        db.collection("events")
                .document(event.getEventId())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(event))
                .addOnFailureListener(callback::onError);
    }

    public void addCoOrganizer(String eventId, String userId, CompletionCallback callback) {
        db.collection("events")
                .document(eventId)
                .update("coOrganizerIds", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void addInvitedUser(String eventId, String userId, CompletionCallback callback) {
        db.collection("events")
                .document(eventId)
                .update("invitedUserIds", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

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
                    callback.onSuccess(buildEventFromDocument(documentSnapshot));
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAllEvents(EventsCallback callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        events.add(buildEventFromDocument(document));
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onError);
    }

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
                    callback.onSuccess(buildEventFromDocument(document));
                })
                .addOnFailureListener(callback::onError);
    }

    @SuppressWarnings("unchecked")
    private Event buildEventFromDocument(DocumentSnapshot document) {
        String title = document.getString("title");
        String description = document.getString("description");
        Long registrationStartMillis = document.getLong("registrationStartMillis");
        Long registrationEndMillis = document.getLong("registrationEndMillis");
        Long capacity = document.getLong("capacity");
        String posterUrl = document.getString("posterImageUrl");
        if (posterUrl == null) {
            posterUrl = document.getString("posterUrl");
        }

        Boolean isPrivate = document.getBoolean("isPrivate");
        List<String> invitedUserIds = (List<String>) document.get("invitedUserIds");
        List<String> coOrganizerIds = (List<String>) document.get("coOrganizerIds");
        String organizerId = document.getString("organizerId");

        if (title == null) title = "";
        if (description == null) description = "";
        if (registrationStartMillis == null) registrationStartMillis = 0L;
        if (registrationEndMillis == null) registrationEndMillis = 0L;
        if (capacity == null || capacity <= 0) capacity = (long) Event.DEFAULT_CAPACITY;
        if (isPrivate == null) isPrivate = false;
        if (invitedUserIds == null) invitedUserIds = new ArrayList<>();
        if (coOrganizerIds == null) coOrganizerIds = new ArrayList<>();
        if (organizerId == null) organizerId = "";

        // Step 2: read geolocationRequired from Firestore
        Boolean geolocationRequired = document.getBoolean("geolocationRequired");
        if (geolocationRequired == null) geolocationRequired = false;

        return new Event(
                document.getId(),
                title,
                description,
                registrationStartMillis,
                registrationEndMillis,
                capacity.intValue(),
                posterUrl,
                isPrivate,
                invitedUserIds,
                coOrganizerIds,
                organizerId,
                geolocationRequired
        );
    }
}