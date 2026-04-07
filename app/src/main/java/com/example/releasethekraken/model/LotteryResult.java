package com.example.releasethekraken.model;

import java.util.List;

/**
 * represents the result of running a lottery for an event
 * stores the list of accepted entrants (winners) and rejected entrants (non-winners)
 * this class belongs to the model layer and contains no UI logic
 */
public class LotteryResult {

    // list of entrant IDs who were selected in the lottery
    public List<String> accepted;

    // list of entrant IDs who were not selected in the lottery
    public List<String> rejected;

    /**
     * creates a new LotteryResult object
     * @param accepted list of entrant IDs who were selected
     * @param rejected list of entrant IDs who were not selected
     */
    public LotteryResult(List<String> accepted, List<String> rejected) {
        this.accepted = accepted;
        this.rejected = rejected;
    }
}