package com.example.releasethekraken.controller;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.example.releasethekraken.controller.NotificationService;
import com.example.releasethekraken.model.Event;
import com.example.releasethekraken.model.EventRepository;
import com.example.releasethekraken.model.LotteryManager;
import com.example.releasethekraken.model.LotteryResult;
import com.example.releasethekraken.model.NotificationRepository;
import com.example.releasethekraken.model.WaitingListRepository;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


//Generated from ChatGPT based on previous implementation "update this code to work with new lotteryManager, and add notifications" 2026-03-23

/**
 * Worker that performs the lottery draw for a given event.
 *
 * <p>It selects winners randomly up to the event capacity, separates entrants
 * into accepted and rejected lists, saves the results, and sends notifications
 * to all entrants (win or lose). The worker completes only after all database
 * operations and notifications are finished.
 *
 * <p>Fails if the event ID is missing or any database operation fails.
 */
public class DrawEntrantsWorker extends ListenableWorker {

    public DrawEntrantsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {

        return CallbackToFutureAdapter.getFuture(completer -> {

            String eventId = getInputData().getString("eventId");

            if (eventId == null) {
                completer.set(Result.failure());
                return "Missing eventId";
            }

            EventRepository eventRepo = new EventRepository();
            WaitingListRepository waitingListRepo = new WaitingListRepository();
            NotificationService notifService = new NotificationService(new NotificationRepository());

            // Step 1: Fetch event
            eventRepo.getEventById(eventId, new EventRepository.EventCallback() {
                @Override
                public void onSuccess(Event event) {

                    // Step 2: Fetch entrants
                    waitingListRepo.getAllEntrants(event.getEventId(), new WaitingListRepository.EntrantsCallback() {
                        @Override
                        public void onResult(List<String> entrants) {

                            if (entrants.isEmpty()) {
                                completer.set(Result.success());
                                return;
                            }

                            int capacity = event.getCapacity();

                            // Step 3: Run lottery
                            LotteryResult result = new LotteryManager()
                                    .drawEntrants(event, entrants, capacity);

                            List<String> winners = result.accepted;
                            List<String> rejected = result.rejected;

                            // Step 4: Save results
                            waitingListRepo.saveDrawnEntrants(
                                    event.getEventId(),
                                    winners,
                                    rejected,
                                    new WaitingListRepository.CompletionCallback() {

                                        @Override
                                        public void onSuccess() {

                                            int totalNotifications = winners.size() + rejected.size();

                                            // If no one to notify, finish immediately
                                            if (totalNotifications == 0) {
                                                completer.set(Result.success());
                                                return;
                                            }

                                            AtomicInteger completedCount = new AtomicInteger(0);

                                            Runnable tryFinish = () -> {
                                                if (completedCount.incrementAndGet() == totalNotifications) {
                                                    completer.set(Result.success());
                                                }
                                            };

                                            // Step 5a: Notify winners
                                            for (String winnerId : winners) {
                                                notifService.sendWinNotification(
                                                        event,
                                                        winnerId,
                                                        new NotificationService.NotificationCallback() {
                                                            @Override
                                                            public void onResult(NotificationService.NotificationResult result) {
                                                                tryFinish.run();
                                                            }

                                                            @Override
                                                            public void onError(Exception e) {
                                                                e.printStackTrace();
                                                                tryFinish.run();
                                                            }
                                                        }
                                                );
                                            }

                                            // Step 5b: Notify rejected entrants
                                            for (String rejectedId : rejected) {
                                                notifService.sendLossNotification(
                                                        event,
                                                        rejectedId,
                                                        new NotificationService.NotificationCallback() {
                                                            @Override
                                                            public void onResult(NotificationService.NotificationResult result) {
                                                                tryFinish.run();
                                                            }

                                                            @Override
                                                            public void onError(Exception e) {
                                                                e.printStackTrace();
                                                                tryFinish.run();
                                                            }
                                                        }
                                                );
                                            }
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            e.printStackTrace();
                                            completer.set(Result.failure());
                                        }
                                    }
                            );
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                            completer.set(Result.failure());
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    completer.set(Result.failure());
                }
            });

            return "DrawEntrantsWorker";
        });
    }
}