package com.example.releasethekraken.model;

import com.example.releasethekraken.controller.NotificationService;

import java.util.ArrayList;
import java.util.Random;

/// Responsibilities                                                    Collaborators
/// ---------------------------------------------------------------------------------------
/// Randomly select entrants from WaitingList up to capacity            WaitingList
/// Handle invitation workflow (invite → accept/decline → refill spot)	Event
/// Support redraw when someone declines or later cancels	            NotificationService
/// Produce final accepted attendee list
public class LotteryManager {

    //Takes in all entrants from an event and capacity, and chooses random people up to its capacity, returns accepted entrants


    NotificationRepository notifRepo = new NotificationRepository();
    NotificationService notifService = new NotificationService(new NotificationRepository());
    public ArrayList<String> drawEntrants(Event event, ArrayList<String> allEntrants, int capacity) {
        Random rand = new Random();
        ArrayList<String> acceptedEntrants = new ArrayList<String>();

        for (int i = 0; (i < capacity) && (!allEntrants.isEmpty()); i++) {
            String acceptedEntrant = allEntrants.remove(rand.nextInt(allEntrants.size()));
            acceptedEntrants.add(acceptedEntrant);

            //Notifications!
            notifService.sendWinNotification(event, acceptedEntrant, new NotificationService.NotificationCallback() {
                @Override
                public void onResult(NotificationService.NotificationResult result) {
                    //add result handling later potentially
                }

                @Override
                public void onError(Exception e) {
                    //add error handling later potentially
                }
            });
        }
        return acceptedEntrants;
    }

    //Takes in remaining entrants from event, accepted entrants and capacity, and fills up the accepted entrants with randomly from remaining entrants
    public ArrayList<String> drawReplacementEntrants(Event event, ArrayList<String> allEntrants, ArrayList<String> acceptedEntrants, int capacity) {
        Random rand = new Random();

        for (int i = acceptedEntrants.size(); (i < capacity) && (!allEntrants.isEmpty()); i++) {
            String acceptedEntrant = allEntrants.remove(rand.nextInt(allEntrants.size()));
            acceptedEntrants.add(acceptedEntrant);

            //Notifications! (Could change this to be more custom later to indicate that you were part of a *redraw*)
            notifService.sendWinNotification(event, acceptedEntrant, new NotificationService.NotificationCallback() {
                @Override
                public void onResult(NotificationService.NotificationResult result) {
                    //add result handling later potentially
                }

                @Override
                public void onError(Exception e) {
                    //add error handling later potentially
                }
            });
        }
        return acceptedEntrants;
    }
}