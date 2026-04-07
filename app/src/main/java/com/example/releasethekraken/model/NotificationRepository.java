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
 * Repository responsible for sending, logging, and retrieving notifications from Firestore.
 * This class belongs to the model layer and contains no UI logic.
 * It manages notification data, handling fetching, updating, and storing notifications
 * from Firebase Firestore, acting as an abstraction layer between the data source
 * and UI components.
 */
public class NotificationRepository {

    private final FirebaseFirestore db;

    /**
     * Timeout for unresponsive invitations (48 hours).
     */
    private static final long UNRESPONSIVE_TIMEOUT_MILLIS = 48L * 60L * 60L * 1000L;

    /**
     * Default constructor that initializes the repository with the default Firestore instance.
     */
    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor that allows providing a specific Firestore instance, useful for testing.
     * @param db The Firestore database instance to use.
     */
    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Callback interface for simple success/error operations.
     */
    public interface CompletionCallback {
        /**
         * Called when the operation completes successfully.
         */
        void onSuccess();
        /**
         * Called when the operation fails.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Callback interface for operations returning a list of notifications.
     */
    public interface NotificationsCallback {
        /**
         * Called when the notifications are successfully retrieved.
         * @param notifications The list of notifications.
         */
        void onSuccess(List<Notification> notifications);
        /**
         * Called when the operation fails.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Callback interface for operations returning a list of entrant IDs.
     */
    public interface EntrantIdsCallback {
        /**
         * Called when the entrant IDs are successfully retrieved.
         * @param entrantIds The list of entrant IDs.
         */
        void onSuccess(List<String> entrantIds);
        /**
         * Called when the operation fails.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Callback interface for operations that cancel unresponsive entrants.
     */
    public interface CancelUnresponsiveCallback {
        /**
         * Called when the operation completes successfully.
         * @param cancelledCount The number of entrants cancelled.
         */
        void onSuccess(int cancelledCount);
        /**
         * Called when the operation fails.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Sends a notification to a specific entrant by adding it to their notifications sub-collection.
     * @param notification The notification to send.
     * @param callback The callback to handle the operation result.
     */
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

    /**
     * Logs a notification in a central "notificationLogs" collection for administrative tracking.
     * @param notification The notification to log.
     * @param callback The callback to handle the operation result.
     */
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

    /**
     * Retrieves all notifications for a specific entrant, ordered by most recent first.
     * @param entrantId The ID of the entrant.
     * @param callback The callback to return the list of notifications.
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

    /**
     * Retrieves a list of entrants who have been invited to an event and have not declined or been cancelled.
     * @param eventId The ID of the event.
     * @param callback The callback to return the filtered list of invitation notifications.
     */
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

    /**
     * Retrieves a list of entrants whose invitations for an event were declined or cancelled.
     * @param eventId The ID of the event.
     * @param callback The callback to return the list of declined/cancelled notifications.
     */
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

    /**
     * Retrieves notifications for an event that match a specific response status.
     * @param eventId The ID of the event.
     * @param status The status to filter by (e.g., "accepted", "pending", "declined").
     * @param callback The callback to return the list of notifications.
     */
    public void getEntrantsByStatus(String eventId, String status, NotificationsCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("notifications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("responseStatus", status)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        notifications.add(doc.toObject(Notification.class));
                    }
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Helper method to retrieve all invitation-type notifications for a specific event across all profiles.
     * @param eventId The ID of the event.
     * @param callback The callback to return the list of invitation notifications.
     */
    private void getInvitationNotificationsForEvent(String eventId, NotificationsCallback callback) {
        db.collectionGroup("notifications")
                .whereEqualTo("eventId", eventId)
                .orderBy("sentAtMillis", Query.Direction.DESCENDING)
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

                        if (entrantId == null) entrantId = "";
                        if (message == null) message = "";
                        if (type == null) type = "";
                        if (sentAtMillis == null) sentAtMillis = 0L;
                        if (read == null) read = false;

                        if (!isSignupInvitationType(type) && !"CO_ORGANIZER".equalsIgnoreCase(type)) {
                            continue;
                        }

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

    /**
     * Checks if a notification type is a signup invitation type.
     * @param type The notification type.
     * @return True if it is a signup invitation type, false otherwise.
     */
    private boolean isSignupInvitationType(String type) {
        return type != null && (
                type.equalsIgnoreCase("WIN")
                        || type.equalsIgnoreCase("SELECTED")
                        || type.equalsIgnoreCase("PRIVATE_INVITE")
                        || type.equalsIgnoreCase("INVITATION")
        );
    }

    /**
     * Normalizes a status string by trimming and converting to lowercase.
     * @param responseStatus The status string to normalize.
     * @return The normalized status string, or "pending" if it was null or empty.
     */
    private String normalizeStatus(String responseStatus) {
        if (responseStatus == null || responseStatus.trim().isEmpty()) {
            return "pending";
        }
        return responseStatus.trim().toLowerCase();
    }

    /**
     * Validates and accepts an invitation, updating Firestore accordingly within a transaction.
     * @param entrantId The ID of the entrant.
     * @param eventId The ID of the event.
     * @param notificationId The ID of the notification.
     * @param callback The callback to handle the operation result.
     */
    public void acceptInvitation(String entrantId,
                                 String eventId,
                                 String notificationId,
                                 CompletionCallback callback) {
        if (callback == null) {
            return;
        }

        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()
                || notificationId == null || notificationId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid invitation data."));
            return;
        }

        validateAndAcceptInvitation(entrantId, eventId, notificationId, callback);
    }

    /**
     * Internal logic to perform the transaction for accepting an invitation.
     */
    private void validateAndAcceptInvitation(String entrantId,
                                             String eventId,
                                             String notificationId,
                                             CompletionCallback callback) {
        db.runTransaction(transaction -> {
                    // 1. ALL READS FIRST
                    DocumentSnapshot notificationSnapshot = transaction.get(
                            db.collection("profiles")
                                    .document(entrantId)
                                    .collection("notifications")
                                    .document(notificationId)
                    );

                    DocumentSnapshot eventSnapshot = transaction.get(db.collection("events").document(eventId));

                    String type = notificationSnapshot.getString("type");
                    DocumentSnapshot waitingListSnapshot = null;
                    if ("PRIVATE_INVITE".equalsIgnoreCase(type)) {
                        waitingListSnapshot = transaction.get(
                                db.collection("events")
                                        .document(eventId)
                                        .collection("waitingList")
                                        .document(entrantId)
                        );
                    }

                    // 2. LOGIC AND VALIDATION
                    if (!notificationSnapshot.exists()) {
                        throw new IllegalStateException("This invitation is no longer available.");
                    }

                    String currentStatus = normalizeStatus(notificationSnapshot.getString("responseStatus"));

                    if (!isInvitationType(type)) {
                        throw new IllegalStateException("This notification cannot be responded to.");
                    }

                    if (!"pending".equals(currentStatus)) {
                        throw new IllegalStateException("This invitation has already been responded to.");
                    }

                    long respondedAtMillis = System.currentTimeMillis();
                    Map<String, Object> notificationUpdates = new HashMap<>();
                    notificationUpdates.put("read", true);
                    notificationUpdates.put("responseStatus", "accepted");
                    notificationUpdates.put("respondedAtMillis", respondedAtMillis);

                    // 3. ALL WRITES AFTER READS
                    transaction.update(
                            db.collection("profiles")
                                    .document(entrantId)
                                    .collection("notifications")
                                    .document(notificationId),
                            notificationUpdates
                    );

                    if ("CO_ORGANIZER".equalsIgnoreCase(type)) {
                        transaction.update(
                                db.collection("events").document(eventId),
                                "coOrganizerIds", FieldValue.arrayUnion(entrantId)
                        );
                        return null;
                    }

                    if (!eventSnapshot.exists()) {
                        throw new IllegalStateException("This event no longer exists.");
                    }

                    if ("PRIVATE_INVITE".equalsIgnoreCase(type)) {
                        if (waitingListSnapshot == null || !waitingListSnapshot.exists()) {
                            Map<String, Object> waitingListData = new HashMap<>();
                            waitingListData.put("eventId", eventId);
                            waitingListData.put("entrantId", entrantId);
                            waitingListData.put("joinedAtMillis", respondedAtMillis);
                            waitingListData.put("latitude", 0.0);
                            waitingListData.put("longitude", 0.0);

                            transaction.set(
                                    db.collection("events")
                                            .document(eventId)
                                            .collection("waitingList")
                                            .document(entrantId),
                                    waitingListData,
                                    SetOptions.merge()
                            );
                        }
                        return null;
                    }

                    if ("WIN".equalsIgnoreCase(type) || "SELECTED".equalsIgnoreCase(type)) {
                        Map<String, Object> acceptedData = new HashMap<>();
                        acceptedData.put("selected", true);
                        acceptedData.put("status", "accepted");
                        acceptedData.put("respondedAtMillis", respondedAtMillis);

                        transaction.set(
                                db.collection("events")
                                        .document(eventId)
                                        .collection("accepted")
                                        .document(entrantId),
                                acceptedData,
                                SetOptions.merge()
                        );
                    }

                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Validates and declines an invitation, updating Firestore using a WriteBatch.
     * If the invitation was for a winning spot, it triggers a replacement selection.
     * @param entrantId The ID of the entrant.
     * @param eventId The ID of the event.
     * @param notificationId The ID of the notification.
     * @param callback The callback to handle the operation result.
     */
    public void declineInvitation(String entrantId,
                                  String eventId,
                                  String notificationId,
                                  CompletionCallback callback) {
        if (callback == null) {
            return;
        }

        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()
                || notificationId == null || notificationId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid invitation data."));
            return;
        }

        db.collection("profiles")
                .document(entrantId)
                .collection("notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onError(new IllegalStateException("This invitation is no longer available."));
                        return;
                    }

                    String type = documentSnapshot.getString("type");
                    String currentStatus = normalizeStatus(documentSnapshot.getString("responseStatus"));

                    if (!isInvitationType(type)) {
                        callback.onError(new IllegalStateException("This notification cannot be responded to."));
                        return;
                    }

                    if (!"pending".equals(currentStatus)) {
                        callback.onError(new IllegalStateException("This invitation has already been responded to."));
                        return;
                    }

                    processDecline(entrantId, eventId, notificationId, type, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Internal logic to process the decline of an invitation.
     */
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

    /**
     * Checks if a notification type is an invitation that can be responded to.
     * @param type The notification type.
     * @return True if it is an invitation type, false otherwise.
     */
    private boolean isInvitationType(String type) {
        return type != null && (
                type.equalsIgnoreCase("WIN")
                        || type.equalsIgnoreCase("SELECTED")
                        || type.equalsIgnoreCase("PRIVATE_INVITE")
                        || type.equalsIgnoreCase("INVITATION")
                        || type.equalsIgnoreCase("CO_ORGANIZER")
        );
    }

    /**
     * Cancels invitations for entrants who have not responded within the default timeout.
     * @param eventId The ID of the event.
     * @param callback The callback to handle the operation result.
     */
    public void cancelUnresponsiveEntrantsForEvent(String eventId, CancelUnresponsiveCallback callback) {
        cancelUnresponsiveEntrantsForEvent(eventId, UNRESPONSIVE_TIMEOUT_MILLIS, callback);
    }

    /**
     * Cancels invitations for entrants who have not responded within a custom timeout.
     * @param eventId The ID of the event.
     * @param timeoutMillis The timeout in milliseconds.
     * @param callback The callback to handle the operation result.
     */
    public void cancelUnresponsiveEntrantsForEvent(String eventId,
                                                   long timeoutMillis,
                                                   CancelUnresponsiveCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid event ID."));
            return;
        }

        getInvitationNotificationsForEvent(eventId, new NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                long nowMillis = System.currentTimeMillis();
                List<Notification> expiredPendingNotifications = new ArrayList<>();

                for (Notification notification : notifications) {
                    String status = normalizeStatus(notification.getResponseStatus());
                    boolean isPending = "pending".equals(status);
                    boolean isExpired = nowMillis - notification.getSentAtMillis() > timeoutMillis;

                    if (isPending && isExpired) {
                        expiredPendingNotifications.add(notification);
                    }
                }

                if (expiredPendingNotifications.isEmpty()) {
                    callback.onSuccess(0);
                    return;
                }

                cancelUnresponsiveSequentially(
                        eventId,
                        expiredPendingNotifications,
                        0,
                        0,
                        callback
                );
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Retrieves a list of entrant IDs who have successfully accepted the invitation for an event.
     * @param eventId The ID of the event.
     * @param callback The callback to return the list of entrant IDs.
     */
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

    /**
     * Internal helper to cancel unresponsive entrants one by one.
     */
    private void cancelUnresponsiveSequentially(String eventId,
                                                List<Notification> notifications,
                                                int index,
                                                int cancelledCount,
                                                CancelUnresponsiveCallback callback) {
        if (index >= notifications.size()) {
            callback.onSuccess(cancelledCount);
            return;
        }

        Notification notification = notifications.get(index);

        cancelSingleUnresponsiveEntrant(
                notification.getEntrantId(),
                eventId,
                notification.getNotificationId(),
                new CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        cancelUnresponsiveSequentially(
                                eventId,
                                notifications,
                                index + 1,
                                cancelledCount + 1,
                                callback
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        cancelUnresponsiveSequentially(
                                eventId,
                                notifications,
                                index + 1,
                                cancelledCount,
                                callback
                        );
                    }
                }
        );
    }

    /**
     * Internal helper to cancel a single unresponsive entrant's invitation.
     */
    private void cancelSingleUnresponsiveEntrant(String entrantId,
                                                 String eventId,
                                                 String notificationId,
                                                 CompletionCallback callback) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()
                || notificationId == null || notificationId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Invalid cancellation data."));
            return;
        }

        db.collection("profiles")
                .document(entrantId)
                .collection("notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onSuccess();
                        return;
                    }

                    String type = documentSnapshot.getString("type");
                    processUnresponsiveCancellation(entrantId, eventId, notificationId, type, callback);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    /**
     * Internal logic to process the cancellation of an unresponsive entrant.
     */
    private void processUnresponsiveCancellation(String entrantId,
                                                 String eventId,
                                                 String notificationId,
                                                 String type,
                                                 CompletionCallback callback) {
        long respondedAtMillis = System.currentTimeMillis();
        WriteBatch batch = db.batch();

        batch.update(
                db.collection("profiles")
                        .document(entrantId)
                        .collection("notifications")
                        .document(notificationId),
                "read", true,
                "responseStatus", "cancelled",
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
                .addOnFailureListener(e -> callback.onError(e));
    }


    /**
     * Automatically selects a replacement entrant from the waiting list when someone declines or is cancelled.
     * @param eventId The ID of the event.
     * @param declinedEntrantId The ID of the entrant who declined or was cancelled.
     * @param callback The callback to handle the operation result.
     */
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
