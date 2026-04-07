package com.example.releasethekraken.view;
import com.example.releasethekraken.R;
import com.bumptech.glide.Glide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<String> imageUrls = new ArrayList<>();
    private Context context;

    public ImageAdapter(Context context) {
        this.context = context;
    }

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
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            String urlToDelete = imageUrls.get(adapterPosition);
                            deleteFromFirebaseStorage(urlToDelete);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    /**
     * Deletes the image from Firebase Storage using its download URL,
     * then removes it from the local list on success.
     * Uses URL-based lookup on success to avoid stale index issues from async operations.
     */
    private void deleteFromFirebaseStorage(String downloadUrl) {
        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(downloadUrl);
            ref.delete()
                    .addOnSuccessListener(unused -> {
                        int currentIndex = imageUrls.indexOf(downloadUrl);
                        if (currentIndex != -1) {
                            imageUrls.remove(currentIndex);
                            notifyItemRemoved(currentIndex);
                        }
                        Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IllegalArgumentException e) {
            Toast.makeText(context, "Invalid image reference", Toast.LENGTH_SHORT).show();
        }
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
}