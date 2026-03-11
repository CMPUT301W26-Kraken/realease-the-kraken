package com.example.releasethekraken

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Listens for Firebase Cloud Messaging events like new messages and token updates.
// Must be registered in AndroidManifest for notifications to work.
class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Runs when Firebase creates or refreshes this device's messaging token.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Called when a new FCM token is generated
    }

    // Runs when the app receives a Firebase message while it is open.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Log the message to confirm FCM is receiving notifications
        Log.d("FCM", "Message received from: ${remoteMessage.from}")
    }
        // triggered when a message is received.
    }
