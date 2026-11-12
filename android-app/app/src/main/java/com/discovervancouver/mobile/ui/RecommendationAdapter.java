package com.discovervancouver.mobile.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.discovervancouver.mobile.R;
import com.discovervancouver.mobile.data.PreferencesStore;
import com.discovervancouver.mobile.model.Recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RecommendationAdapter
    extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    private final List<Recommendation> recommendations = new ArrayList<>();
    private final PreferencesStore preferencesStore;

    public RecommendationAdapter(PreferencesStore preferencesStore) {
        this.preferencesStore = preferencesStore;
    }

    public void submitList(List<Recommendation> newRecommendations) {
        int previousSize = recommendations.size();
        recommendations.clear();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        recommendations.addAll(newRecommendations);
        if (!newRecommendations.isEmpty()) {
            notifyItemRangeInserted(0, newRecommendations.size());
        }
    }

    public void refreshFavourites() {
        notifyItemRangeChanged(0, recommendations.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
        @NonNull ViewGroup parent,
        int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
        @NonNull ViewHolder holder,
        int position
    ) {
        Recommendation recommendation = recommendations.get(position);
        Context context = holder.itemView.getContext();

        holder.name.setText(
            preferencesStore.isFavourite(recommendation.getPlaceId())
                ? context.getString(
                    R.string.favourite_place_name,
                    recommendation.getName()
                )
                : recommendation.getName()
        );
        holder.openStatus.setText(
            recommendation.isOpenNow() ? "Open" : "Closed"
        );
        holder.openStatus.setTextColor(ContextCompat.getColor(
            context,
            recommendation.isOpenNow() ? R.color.success : R.color.closed
        ));
        holder.meta.setText(String.format(
            Locale.CANADA,
            "%s  •  %d min walk  •  score %d",
            recommendation.getCategory(),
            recommendation.getWalkingMinutes(),
            recommendation.getScore()
        ));
        holder.description.setText(recommendation.getDescription());
        holder.reason.setText(
            recommendation.getReasons().isEmpty()
                ? "Nearby and worth exploring"
                : recommendation.getReasons().get(0)
        );
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, PlaceDetailActivity.class);
            intent.putExtra(PlaceDetailActivity.EXTRA_RECOMMENDATION, recommendation);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView openStatus;
        private final TextView meta;
        private final TextView description;
        private final TextView reason;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.placeName);
            openStatus = itemView.findViewById(R.id.openStatus);
            meta = itemView.findViewById(R.id.placeMeta);
            description = itemView.findViewById(R.id.placeDescription);
            reason = itemView.findViewById(R.id.placeReason);
        }
    }
}
