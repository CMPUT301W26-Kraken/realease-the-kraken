package com.example.releasethekraken.model;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * repository responsible for sending, logging, and retrieving notifications
 * from Firestore
 * This class belongs to the model layer and contains no UI logic
 */
public class NotificationRepository {

    private final FirebaseFirestore db;
    /**
     * creates a NotificationRepository using the default Firestore instance
     */
    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }
    /**
     * creates a NotificationRepository with a specific Firestore instance
     * @param db the Firestore database instance to use
     */
    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }
    /**
     * callback interface for operations that report completion status
     */
    public interface CompletionCallback {
        void onSuccess();
        void onError(Exception e);
    }
    /**
     * callback interface for retrieving a list of notifications
     */
    public interface NotificationsCallback {
        void onSuccess(List<Notification> notifications);
        void onError(Exception e);
    }
    /**
     * sends a notification to an entrant by storing it in that entrants
     * notifications collection in Firestore
     * @param notification the notification to send
     * @param callback callback used to report success or failure
     */
    public void sendNotification(Notification notification, CompletionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", notification.getEntrantId());
        data.put("eventId", notification.getEventId());
        data.put("message", notification.getMessage());
        data.put("type", notification.getType());
        data.put("sentAtMillis", notification.getSentAtMillis());
        data.put("read", false);

        db.collection("profiles")
                .document(notification.getEntrantId())
                .collection("notifications")
                .add(data)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    /**
     * logs a notification event in the global notificationLogs collection
     *
     * @param notification the notification to log
     * @param callback callback used to report success or failure
     */
    public void logNotification(Notification notification, CompletionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", notification.getEntrantId());
        data.put("eventId", notification.getEventId());
        data.put("message", notification.getMessage());
        data.put("type", notification.getType());
        data.put("sentAtMillis", notification.getSentAtMillis());

        db.collection("notificationLogs")
                .add(data)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    /**
     * retrieves all notifications for a specific entrant from Firestore,
     * ordered by sent time from newest to oldest
     * @param entrantId the ID of the entrant whose notifications are being retrieved
     * @param callback callback used to return the notifications or an error
     */
    public void getNotificationsForEntrant(String entrantId, NotificationsCallback callback) {
        db.collection("profiles")
                .document(entrantId)
                .collection("notifications")
                .orderBy("sentAtMillis", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String eventId = document.getString("eventId");
                        String message = document.getString("message");
                        String type = document.getString("type");
                        Long sentAtMillis = document.getLong("sentAtMillis");

                        if (eventId == null) {
                            eventId = "";
                        }
                        if (message == null) {
                            message = "";
                        }
                        if (type == null) {
                            type = "";
                        }
                        if (sentAtMillis == null) {
                            sentAtMillis = 0L;
                        }

                        Notification notification = new Notification(
                                entrantId,
                                eventId,
                                message,
                                type,
                                sentAtMillis
                        );

                        notifications.add(notification);
                    }

                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(callback::onError);
    }
}