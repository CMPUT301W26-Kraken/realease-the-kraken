package com.example.releasethekraken.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.releasethekraken.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Admin-only fragment that displays a log of all notifications sent by the system.
 * Reads from the top-level "notificationLogs" Firestore collection.
 * Shows recipient (entrantId), event title, message type, and timestamp.
 * this file was created with the help of generative AI.
 */
public class AdminNotificationLogFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private final List<LogEntry> entries = new ArrayList<>();

    public AdminNotificationLogFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_notification_log, container, false);

        recyclerView = view.findViewById(R.id.notification_log_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LogAdapter(entries);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.home_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_mainMenuFragment));

        view.findViewById(R.id.profile_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_viewProfileFragment));

        view.findViewById(R.id.notifications_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_global_notificationFragment));

        fetchNotificationLogs();

        return view;
    }

    private void fetchNotificationLogs() {
        FirebaseFirestore.getInstance()
                .collection("notificationLogs")
                .orderBy("sentAtMillis", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    entries.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String entrantId   = doc.getString("entrantId");
                        String eventTitle  = doc.getString("eventTitle");
                        String type        = doc.getString("type");
                        Long sentAtMillis  = doc.getLong("sentAtMillis");

                        entries.add(new LogEntry(
                                entrantId   != null ? entrantId  : "Unknown",
                                eventTitle  != null ? eventTitle : doc.getString("eventId"),
                                type        != null ? type       : "Unknown",
                                sentAtMillis != null ? sentAtMillis : 0L
                        ));
                    }

                    adapter.notifyDataSetChanged();

                    if (entries.isEmpty()) {
                        Toast.makeText(requireContext(), "No notification logs found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Data model ──────────────────────────────────────────────────────────

    static class LogEntry {
        final String entrantId;
        final String eventTitle;
        final String type;
        final long sentAtMillis;

        LogEntry(String entrantId, String eventTitle, String type, long sentAtMillis) {
            this.entrantId    = entrantId;
            this.eventTitle   = eventTitle;
            this.type         = type;
            this.sentAtMillis = sentAtMillis;
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

        private final List<LogEntry> entries;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault());

        LogAdapter(List<LogEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(32, 20, 32, 20);

            TextView typeView = new TextView(parent.getContext());
            typeView.setTextSize(15);
            typeView.setTextColor(0xFF1E293B);
            typeView.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView recipientView = new TextView(parent.getContext());
            recipientView.setTextSize(13);
            recipientView.setTextColor(0xFF475569);

            TextView eventView = new TextView(parent.getContext());
            eventView.setTextSize(13);
            eventView.setTextColor(0xFF475569);

            TextView timeView = new TextView(parent.getContext());
            timeView.setTextSize(12);
            timeView.setTextColor(0xFF94A3B8);

            row.addView(typeView);
            row.addView(recipientView);
            row.addView(eventView);
            row.addView(timeView);

            return new LogViewHolder(row, typeView, recipientView, eventView, timeView);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            LogEntry entry = entries.get(position);
            holder.typeView.setText("Type: " + entry.type);
            holder.recipientView.setText("Recipient: " + entry.entrantId);
            holder.eventView.setText("Event: " + (entry.eventTitle != null ? entry.eventTitle : "—"));
            holder.timeView.setText(dateFormat.format(new Date(entry.sentAtMillis)));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        static class LogViewHolder extends RecyclerView.ViewHolder {
            TextView typeView;
            TextView recipientView;
            TextView eventView;
            TextView timeView;

            LogViewHolder(@NonNull View itemView, TextView typeView,
                          TextView recipientView, TextView eventView, TextView timeView) {
                super(itemView);
                this.typeView      = typeView;
                this.recipientView = recipientView;
                this.eventView     = eventView;
                this.timeView      = timeView;
            }
        }
    }
}