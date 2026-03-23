package com.example.releasethekraken;

import static org.junit.Assert.assertEquals;

import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.LotteryManager;

import org.junit.Test;

public class LotteryManagerTest {
    //public ArrayList<String> drawEntrants(Event event, ArrayList<String> allEntrants, int capacity)
    //public Event(String eventId, String title, String description, long registrationStartMillis, long registrationEndMillis)
    @Test
    public void selects_to_capacity() {
        //setup
        LotteryManager lotteryManager = new LotteryManager();
        Event event = new Event("0", "event title", "event desc", 0, 1);

    }
}
