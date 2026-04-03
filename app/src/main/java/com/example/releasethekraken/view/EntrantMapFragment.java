package com.example.releasethekraken.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.releasethekraken.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a map showing where entrants joined the waiting list for an event.
 * Only shows markers for entrants whose location was captured.
 * Organizer-only view.
 *
 * NOTE: This file was written with the assistance of Claude (Anthropic) AI.
 */
public class EntrantMapFragment extends Fragment implements OnMapReadyCallback {

    private String eventId;
    private GoogleMap googleMap;
    private TextView noLocationsText;
    private final List<LatLng> locations = new ArrayList<>();

    public EntrantMapFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        noLocationsText = view.findViewById(R.id.map_no_locations_text);

        Button backButton = view.findViewById(R.id.map_back_button);
        backButton.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        // initialise the map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        loadEntrantLocations();
    }

    /**
     * Fetches all waiting list entries for the event and plots markers
     * for those that have valid location data.
     */
    private void loadEntrantLocations() {
        if (eventId == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || googleMap == null) {
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");

                        // skip entries with no location or default 0,0 coords
                        if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) {
                            continue;
                        }

                        LatLng position = new LatLng(lat, lng);
                        locations.add(position);
                        googleMap.addMarker(new MarkerOptions().position(position));
                    }

                    if (locations.isEmpty()) {
                        noLocationsText.setVisibility(View.VISIBLE);
                    } else {
                        zoomToFitMarkers();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Failed to load entrant locations", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Adjusts the camera to fit all markers on screen.
     */
    private void zoomToFitMarkers() {
        if (locations.size() == 1) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locations.get(0), 12f));
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng loc : locations) {
            boundsBuilder.include(loc);
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
    }
}