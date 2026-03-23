package com.example.releasethekraken.model;

import java.util.List;

public class LotteryResult {
    public List<String> accepted;
    public List<String> rejected;

    public LotteryResult(List<String> accepted, List<String> rejected) {
        this.accepted = accepted;
        this.rejected = rejected;
    }
}
