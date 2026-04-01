package com.example.releasethekraken.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.releasethekraken.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * ImageListFragment is a fragment that displays all of the images on the platform for admin users
 * to view and remove images that they find problematic on the platform.
 */
public class ImageListFragment extends Fragment {

    private ImageAdapter adapter;
    private List<String> imageUrls = new ArrayList<>();

    public ImageListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_image_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.images_recycler_view);

        // 2 images per row
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Create adapter
        adapter = new ImageAdapter(getContext());
        recyclerView.setAdapter(adapter);

        fetchImagesFromStorage();

        // Return to Main Menu from Toolbar
        view.findViewById(R.id.home_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_imageListFragment_to_mainMenuFragment)
                );

        // Go to Profile View from Toolbar
        view.findViewById(R.id.profile_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_imageListFragment_to_viewProfileFragment)
                );

        // Navigate to Notifications
        view.findViewById(R.id.notifications_toolbar_button)
                .setOnClickListener(v ->
                        Navigation.findNavController(v)
                                .navigate(R.id.action_imageListFragment_to_notificationFragment)
                );

        return view;
    }

    private void fetchImagesFromStorage() {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // List of folders to fetch
        String[] folders = {"event_posters", "profile_images"};

        for (String folder : folders) {
            StorageReference folderRef = storage.getReference().child(folder);
            folderRef.listAll().addOnSuccessListener(listResult -> {
                for (StorageReference itemRef : listResult.getItems()) {
                    itemRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        imageUrls.add(uri.toString());
                        adapter.setImages(imageUrls); // update adapter as each URL is added
                    });
                }
            }).addOnFailureListener(e -> {
                // Optional: log failure
                e.printStackTrace();
            });
        }
    }
}