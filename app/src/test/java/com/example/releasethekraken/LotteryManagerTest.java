package com.example.releasethekraken;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.LotteryManager;
import com.example.releasethekraken.model.LotteryResult;

import org.junit.Test;

import java.util.Arrays;

public class LotteryManagerTest {
    @Test
    public void selects_to_capacity() {
        LotteryManager lotteryManager = new LotteryManager();
        Event event = new Event("0", "event title", "event desc", 0, 1);
        LotteryResult result = lotteryManager.drawEntrants(
                event,
                Arrays.asList("a", "b", "c", "d"),
                2
        );

        assertEquals(2, result.accepted.size());
        assertEquals(2, result.rejected.size());
        assertTrue(result.accepted.contains("a") || result.rejected.contains("a"));
    }
}
