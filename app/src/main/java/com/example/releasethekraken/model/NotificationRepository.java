package com.example.releasethekraken.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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

    public interface EntrantIdsCallback {
        void onSuccess(List<String> entrantIds);
        void onError(Exception e);
    }

    public void sendNotification(Notification notification, CompletionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", notification.getEntrantId());
        data.put("eventId", notification.getEventId());
        data.put("eventTitle", notification.getEventTitle()); // Store title
        data.put("message", notification.getMessage());
        data.put("type", notification.getType());
        data.put("sentAtMillis", notification.getSentAtMillis());
        data.put("read", false);
        data.put("responseStatus", "pending");

        db.collection("profiles")
                .document(notification.getEntrantId())
                .collection("notifications")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    public void logNotification(Notification notification, CompletionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", notification.getEntrantId());
        data.put("eventId", notification.getEventId());
        data.put("eventTitle", notification.getEventTitle());
        data.put("message", notification.getMessage());
        data.put("type", notification.getType());
        data.put("sentAtMillis", notification.getSentAtMillis());
        data.put("responseStatus", notification.getResponseStatus());

        db.collection("notificationLogs")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
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
                        String eventTitle = document.getString("eventTitle");
                        String message = document.getString("message");
                        String type = document.getString("type");
                        Long sentAtMillis = document.getLong("sentAtMillis");
                        Boolean read = document.getBoolean("read");
                        String responseStatus = document.getString("responseStatus");

                        if (eventId == null) eventId = "";
                        if (message == null) message = "";
                        if (type == null) type = "";
                        if (sentAtMillis == null) sentAtMillis = 0L;
                        if (read == null) read = false;

                        Notification notification = new Notification(
                                document.getId(),
                                entrantId,
                                eventId,
                                eventTitle,
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

    public void getInvitedEntrantsForEvent(String eventId, NotificationsCallback callback) {
        getInvitationNotificationsForEvent(eventId, new NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                List<Notification> filtered = new ArrayList<>();
                for (Notification notification : notifications) {
                    String responseStatus = normalizeStatus(notification.getResponseStatus());
                    if (!"declined".equals(responseStatus) && !"cancelled".equals(responseStatus)) {
                        filtered.add(notification);
                    }
                }
                callback.onSuccess(filtered);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void getCancelledEntrantsForEvent(String eventId, NotificationsCallback callback) {
        getInvitationNotificationsForEvent(eventId, new NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                List<Notification> filtered = new ArrayList<>();
                for (Notification notification : notifications) {
                    String responseStatus = normalizeStatus(notification.getResponseStatus());
                    if ("declined".equals(responseStatus) || "cancelled".equals(responseStatus)) {
                        filtered.add(notification);
                    }
                }
                callback.onSuccess(filtered);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void getFinalAcceptedEntrantsForEvent(String eventId, EntrantIdsCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid event ID."));
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("accepted")
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> entrantIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        entrantIds.add(document.getId());
                    }
                    callback.onSuccess(entrantIds);
                })
                .addOnFailureListener(callback::onError);
    }

    private void getInvitationNotificationsForEvent(String eventId, NotificationsCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid event ID."));
            return;
        }

        db.collectionGroup("notifications")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String entrantId = document.getString("entrantId");
                        String eventTitle = document.getString("eventTitle");
                        String message = document.getString("message");
                        String type = document.getString("type");
                        Long sentAtMillis = document.getLong("sentAtMillis");
                        Boolean read = document.getBoolean("read");
                        String responseStatus = document.getString("responseStatus");

                        if (entrantId == null || entrantId.trim().isEmpty()) {
                            continue;
                        }

                        if (!isSignupInvitationType(type)) {
                            continue;
                        }

                        if (message == null) message = "";
                        if (type == null) type = "";
                        if (sentAtMillis == null) sentAtMillis = 0L;
                        if (read == null) read = false;

                        Notification notification = new Notification(
                                document.getId(),
                                entrantId,
                                eventId,
                                eventTitle,
                                message,
                                type,
                                sentAtMillis,
                                read,
                                responseStatus
                        );

                        notifications.add(notification);
                    }

                    notifications.sort((first, second) ->
                            Long.compare(second.getSentAtMillis(), first.getSentAtMillis()));

                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(callback::onError);
    }

    private boolean isSignupInvitationType(String type) {
        return type != null && (
                type.equalsIgnoreCase("WIN")
                        || type.equalsIgnoreCase("SELECTED")
                        || type.equalsIgnoreCase("PRIVATE_INVITE")
                        || type.equalsIgnoreCase("INVITATION")
        );
    }

    private String normalizeStatus(String responseStatus) {
        if (responseStatus == null || responseStatus.trim().isEmpty()) {
            return "pending";
        }
        return responseStatus.trim().toLowerCase();
    }

    public void acceptInvitation(String entrantId,
                                 String eventId,
                                 String notificationId,
                                 CompletionCallback callback) {
        if (entrantId == null || eventId == null || notificationId == null) {
            callback.onError(new IllegalArgumentException("Invalid invitation data."));
            return;
        }

        db.collection("profiles")
                .document(entrantId)
                .collection("notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String type = documentSnapshot.getString("type");
                    processAcceptance(entrantId, eventId, notificationId, type, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    private void processAcceptance(String entrantId, String eventId, String notificationId, String type, CompletionCallback callback) {
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

        if ("CO_ORGANIZER".equalsIgnoreCase(type)) {
            batch.update(
                    db.collection("events").document(eventId),
                    "coOrganizerIds", FieldValue.arrayUnion(entrantId)
            );
        } else if ("WIN".equalsIgnoreCase(type) || "SELECTED".equalsIgnoreCase(type)) {
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
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void declineInvitation(String entrantId,
                                  String eventId,
                                  String notificationId,
                                  CompletionCallback callback) {
        if (entrantId == null || eventId == null || notificationId == null) {
            callback.onError(new IllegalArgumentException("Invalid invitation data."));
            return;
        }

        db.collection("profiles")
                .document(entrantId)
                .collection("notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String type = documentSnapshot.getString("type");
                    processDecline(entrantId, eventId, notificationId, type, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    private void processDecline(String entrantId, String eventId, String notificationId, String type, CompletionCallback callback) {
        long respondedAtMillis = System.currentTimeMillis();
        WriteBatch batch = db.batch();

        batch.update(
                db.collection("profiles")
                        .document(entrantId)
                        .collection("notifications")
                        .document(notificationId),
                "read", true,
                "responseStatus", "declined",
                "respondedAtMillis", respondedAtMillis
        );

        if ("PRIVATE_INVITE".equalsIgnoreCase(type)) {
            batch.update(
                    db.collection("events").document(eventId),
                    "invitedUserIds", FieldValue.arrayRemove(entrantId)
            );
        } else if ("WIN".equalsIgnoreCase(type) || "SELECTED".equalsIgnoreCase(type)) {
            batch.delete(
                    db.collection("events")
                            .document(eventId)
                            .collection("accepted")
                            .document(entrantId)
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if ("WIN".equalsIgnoreCase(type) || "SELECTED".equalsIgnoreCase(type)) {
                        triggerReplacementSelection(eventId, entrantId, callback);
                    } else {
                        callback.onSuccess();
                    }
                })
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
                                        "A spot opened up for event " + eventId.replace("_", " ")
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