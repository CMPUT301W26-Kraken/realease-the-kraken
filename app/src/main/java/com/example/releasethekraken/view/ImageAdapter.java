package com.example.releasethekraken.view;
import com.example.releasethekraken.R;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<String> imageUrls = new ArrayList<>();
    private Context context;

    public ImageAdapter(Context context) { this.context = context; }

    public void setImages(List<String> urls) {
        this.imageUrls = urls;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.imageView);

        // Click listener for delete confirmation
        holder.itemView.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(R.string.delete_image_title)
                    .setMessage(R.string.delete_image_message)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        int adapterPosition = holder.getAdapterPosition();
                        if (adapterPosition == RecyclerView.NO_POSITION) return;

                        String deleteImageUrl = imageUrls.get(adapterPosition);

                        String path = Uri.parse(deleteImageUrl).getPath();

                        if (path.contains("event_posters")) {
                            nullAndDeleteImage("events", "posterImageUrl", deleteImageUrl, adapterPosition);
                        } else if (path.contains("profile_images")) {
                            nullAndDeleteImage("profiles", "profileImageUrl", deleteImageUrl, adapterPosition);
                        }

                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.item_image_view);
        }
    }

    private void nullAndDeleteImage(String collection, String attribute, String url, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // First set the image URL field to null so that it isn't referenced and found in Glide's cache
        db.collection(collection)
                .whereEqualTo(attribute, url)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        DocumentReference docRef = doc.getReference();
                        docRef.update(attribute, null)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FIRESTORE", "Field cleared successfully");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FIRESTORE", "Failed to clear field", e);
                                });
                        // Because events for some reason have two fields pertaining to URLs and I haven't the time to get to the bottom of it
                        if (collection.equals("events")) {
                            docRef.update("posterUrl", null)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("FIRESTORE", "Field cleared successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("FIRESTORE", "Failed to clear field", e);
                                    });
                        }
                    }

                    // Delete from Storage after nulling the field
                    StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);
                    storageRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                imageUrls.remove(position);
                                notifyItemRemoved(position);
                                Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to find associated objects", Toast.LENGTH_SHORT).show();
                });
    }
}