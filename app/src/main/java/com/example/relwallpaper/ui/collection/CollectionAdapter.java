package com.example.relwallpaper.ui.collection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relwallpaper.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder> {

    private List<CollectionItem> collections;
    private Context context;
    private OnCollectionClickListener listener;
    private boolean isLoading = true;

    public interface OnCollectionClickListener {
        void onCollectionClick(CollectionItem collection);
    }

    public CollectionAdapter(Context context) {
        this.context = context;
        this.collections = new ArrayList<>();
    }

    public void updateCollections(CollectionItem[] newCollections) {
        this.collections.clear();
        this.collections.addAll(Arrays.asList(newCollections));
        this.isLoading = false;
        notifyDataSetChanged();
    }

    public void setInitialCollections(CollectionItem[] initialCollections) {
        this.collections.clear();
        this.collections.addAll(Arrays.asList(initialCollections));
        this.isLoading = true;
        notifyDataSetChanged();
    }

    public void setOnCollectionClickListener(OnCollectionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tag_card, parent, false);
        return new CollectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        CollectionItem collection = collections.get(position);
        holder.bind(collection, isLoading);
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }

    class CollectionViewHolder extends RecyclerView.ViewHolder {
        private ImageView collectionImage;
        private TextView collectionTitle;
        private TextView collectionDescription;
        private TextView collectionCount;

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);
            collectionImage = itemView.findViewById(R.id.collection_image);
            collectionTitle = itemView.findViewById(R.id.collection_title);
            collectionDescription = itemView.findViewById(R.id.collection_description);
            collectionCount = itemView.findViewById(R.id.collection_count);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCollectionClick(collections.get(getAdapterPosition()));
                }
            });
        }

        public void bind(CollectionItem collection, boolean isLoading) {
            collectionTitle.setText(collection.getTitle());
            collectionDescription.setText(collection.getDescription());

            if (isLoading) {
                collectionCount.setText("Loading items...");
            } else {
                collectionCount.setText(collection.getItemCount() + " items");
            }

            collectionImage.setImageResource(collection.getImageResource());
        }
    }
}