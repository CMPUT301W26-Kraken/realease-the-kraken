package com.example.releasethekraken.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * repository responsible for sending, logging, and retrieving notifications
 * from Firestore
 * This class belongs to the model layer and contains no UI logic
 */
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
        data.put("responseStatus", notification.getResponseStatus());

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
        data.put("responseStatus", notification.getResponseStatus());

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
                        Boolean read = document.getBoolean("read");
                        String responseStatus = document.getString("responseStatus");

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
                        if (read == null) {
                            read = false;
                        }

                        Notification notification = new Notification(
                                document.getId(),
                                entrantId,
                                eventId,
                                message,
                                type,
                                sentAtMillis,
                                read,
                                responseStatus
                        );

                        notifications.add(notification);
                    }

                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(callback::onError);
    }

    public void acceptInvitation(String entrantId,
                                 String eventId,
                                 String notificationId,
                                 CompletionCallback callback) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()
                || notificationId == null || notificationId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid invitation data."));
            return;
        }

        long respondedAtMillis = System.currentTimeMillis();
        WriteBatch batch = db.batch();

        batch.update(
                db.collection("profiles")
                        .document(entrantId)
                        .collection("notifications")
                        .document(notificationId),
                "read", true,
                "responseStatus", "accepted",
                "respondedAtMillis", respondedAtMillis
        );

        Map<String, Object> acceptedData = new HashMap<>();
        acceptedData.put("selected", true);
        acceptedData.put("status", "accepted");
        acceptedData.put("respondedAtMillis", respondedAtMillis);

        batch.set(
                db.collection("events")
                        .document(eventId)
                        .collection("accepted")
                        .document(entrantId),
                acceptedData,
                SetOptions.merge()
        );

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void declineInvitation(String entrantId,
                                  String eventId,
                                  String notificationId,
                                  CompletionCallback callback) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()
                || notificationId == null || notificationId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid invitation data."));
            return;
        }

        long respondedAtMillis = System.currentTimeMillis();

        WriteBatch declineBatch = db.batch();

        declineBatch.update(
                db.collection("profiles")
                        .document(entrantId)
                        .collection("notifications")
                        .document(notificationId),
                "read", true,
                "responseStatus", "declined",
                "respondedAtMillis", respondedAtMillis
        );

        declineBatch.delete(
                db.collection("events")
                        .document(eventId)
                        .collection("accepted")
                        .document(entrantId)
        );

        declineBatch.commit()
                .addOnSuccessListener(unused -> triggerReplacementSelection(eventId, entrantId, callback))
                .addOnFailureListener(callback::onError);
    }

    private void triggerReplacementSelection(String eventId,
                                             String declinedEntrantId,
                                             CompletionCallback callback) {

        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(waitingListSnapshot -> {
                    List<String> waitingListIds = new ArrayList<>();
                    for (DocumentSnapshot doc : waitingListSnapshot.getDocuments()) {
                        waitingListIds.add(doc.getId());
                    }

                    db.collection("events")
                            .document(eventId)
                            .collection("accepted")
                            .get()
                            .addOnSuccessListener(acceptedSnapshot -> {
                                List<String> acceptedIds = new ArrayList<>();
                                for (DocumentSnapshot doc : acceptedSnapshot.getDocuments()) {
                                    acceptedIds.add(doc.getId());
                                }

                                List<String> replacementCandidates = new ArrayList<>();
                                for (String entrantId : waitingListIds) {
                                    if (!entrantId.equals(declinedEntrantId) && !acceptedIds.contains(entrantId)) {
                                        replacementCandidates.add(entrantId);
                                    }
                                }

                                if (replacementCandidates.isEmpty()) {
                                    callback.onSuccess();
                                    return;
                                }

                                String replacementEntrantId = replacementCandidates.get(
                                        new Random().nextInt(replacementCandidates.size())
                                );

                                long nowMillis = System.currentTimeMillis();

                                WriteBatch replacementBatch = db.batch();

                                Map<String, Object> acceptedData = new HashMap<>();
                                acceptedData.put("selected", true);
                                acceptedData.put("status", "pending");
                                acceptedData.put("selectedAtMillis", nowMillis);
                                acceptedData.put("replacementFor", declinedEntrantId);

                                replacementBatch.set(
                                        db.collection("events")
                                                .document(eventId)
                                                .collection("accepted")
                                                .document(replacementEntrantId),
                                        acceptedData,
                                        SetOptions.merge()
                                );

                                replacementBatch.delete(
                                        db.collection("events")
                                                .document(eventId)
                                                .collection("rejected")
                                                .document(replacementEntrantId)
                                );

                                Map<String, Object> notificationData = new HashMap<>();
                                notificationData.put("entrantId", replacementEntrantId);
                                notificationData.put("eventId", eventId);
                                notificationData.put(
                                        "message",
                                        "A spot opened up for event " + eventId
                                                + ". You have been invited to sign up. Please respond in the app."
                                );
                                notificationData.put("type", "SELECTED");
                                notificationData.put("sentAtMillis", nowMillis);
                                notificationData.put("read", false);
                                notificationData.put("responseStatus", "pending");

                                replacementBatch.set(
                                        db.collection("profiles")
                                                .document(replacementEntrantId)
                                                .collection("notifications")
                                                .document(),
                                        notificationData
                                );

                                replacementBatch.set(
                                        db.collection("notificationLogs")
                                                .document(),
                                        notificationData
                                );

                                replacementBatch.commit()
                                        .addOnSuccessListener(unused -> callback.onSuccess())
                                        .addOnFailureListener(callback::onError);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
}