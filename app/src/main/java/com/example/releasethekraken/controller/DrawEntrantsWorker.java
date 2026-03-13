package com.example.releasethekraken.controller;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DrawEntrantsWorker extends Worker {

    public DrawEntrantsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        String eventId = getInputData().getString("eventId");

        if (eventId != null) {
            //Code to draw entrants
        }

        return Result.success();
    }
}
