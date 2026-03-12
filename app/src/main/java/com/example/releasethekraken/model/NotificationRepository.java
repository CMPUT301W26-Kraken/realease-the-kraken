package com.example.releasethekraken.model;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//this is for logging and sending notifications through firebase
//this is in the model package
//no UI logic

//repository for sending and logging notifications
public class NotificationRepository {

    private final FirebaseFirestore db;

    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public interface CompletionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface NotificationsCallback {
        void onSuccess(List<Notification> notifications);
        void onError(Exception e);
    }

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