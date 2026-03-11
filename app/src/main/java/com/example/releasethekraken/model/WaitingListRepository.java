package com.example.releasethekraken.model;

//responsible for saving and retrieving waiting list entries data
//this is in the model package
//no UI logic
//will talk to Firebase for data

// TODO:**FIRESTORE METHOD STILL NEEDS TO BE ADDED IN THE COMMENTS BELOW IN THE CODE**
public class WaitingListRepository {

    //returns true if the entrant is already on the waiting list
    public boolean isEntrantAlreadyWaiting(String eventId, String entrantId) {
        // TODO: replace with Firestore query
        return false;
    }

    //saves a new waiting list entry for event as data in Firestore
    public void addToWaitingList(WaitingListEntry entry) {
        // TODO: replace with Firestore write
    }

    //removes a waiting list entry for event as data in Firestore
    public void removeFromWaitingList(String eventId, String entrantId) {
        // TODO: replace with Firestore write

    }

}