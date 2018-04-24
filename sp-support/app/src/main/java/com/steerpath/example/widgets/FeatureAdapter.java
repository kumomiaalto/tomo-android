package com.steerpath.example.widgets;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.steerpath.example.R;
import com.steerpath.sdk.meta.MetaFeature;

import java.util.ArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 *
 */

public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.BuildingViewHolder> {

    private ArrayList<MetaFeature> features = new ArrayList<>();
    private final View.OnClickListener onClickListener;
    private final boolean showCheckBoxes;
    private boolean[] checkedStatus = new boolean[0];
    private Callback callback;

    public interface Callback {
        void onFeatureCheckBoxClicked();
    }

    public FeatureAdapter(View.OnClickListener listener, boolean showCheckBoxes) {
        this.onClickListener = listener;
        this.showCheckBoxes = showCheckBoxes;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public BuildingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BuildingViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.feature_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final BuildingViewHolder holder, int position) {
        MetaFeature feature = features.get(position);
        if (!showCheckBoxes) {
            holder.button.setVisibility(VISIBLE);
            holder.checkbox.setVisibility(GONE);
            holder.floor.setVisibility(GONE);
            holder.description.setVisibility(GONE);
            holder.id.setVisibility(GONE);
            holder.divider.setVisibility(GONE);
        } else {
            holder.button.setVisibility(GONE);
            holder.checkbox.setVisibility(VISIBLE);
            holder.floor.setVisibility(VISIBLE);
            holder.description.setVisibility(VISIBLE);
            holder.id.setVisibility(VISIBLE);
            holder.divider.setVisibility(VISIBLE);

            //holder.bind should not trigger onCheckedChanged, it should just update UI
            holder.checkbox.setOnCheckedChangeListener(null);
            holder.checkbox.setChecked(checkedStatus[position]);
            holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    checkedStatus[holder.getAdapterPosition()] = isChecked;
                    if (callback != null) {
                        callback.onFeatureCheckBoxClicked();
                    }
                }
            });
        }

        if (!feature.getTitle().isEmpty()) {
            holder.button.setText(feature.getTitle());
            holder.checkbox.setText(feature.getTitle());

        } else if (!feature.getId().isEmpty()) {
            holder.button.setText(feature.getId());
            holder.checkbox.setText(feature.getId());

        } else {
            // make sure that some text is on the screen
            String noTitle = "No title";
            holder.button.setText(noTitle);
            holder.checkbox.setText(noTitle);
        }

        String format = holder.description.getResources().getString(R.string.tags);
        String tags = TextUtils.join(", ", feature.getTags());
        holder.description.setText(String.format(format, tags));

        format = holder.floor.getResources().getString(R.string.floor);
        holder.floor.setText(String.format(format, feature.getFloor()));

        format = holder.id.getResources().getString(R.string.id);
        holder.id.setText(String.format(format, feature.getId()));

        holder.button.setOnClickListener(onClickListener);
        holder.button.setTag(feature);
    }

    @Override
    public int getItemCount() {
        return features.size();
    }

    public void setData(ArrayList<MetaFeature> features) {
        this.features = features;
        checkedStatus = new boolean[features.size()];
        notifyDataSetChanged();
    }

    public ArrayList<MetaFeature> getSelectedFeatures() {
        ArrayList<MetaFeature> result = new ArrayList<>();
        for (int i=0; i<features.size(); i++) {
            if (checkedStatus[i]) {
                result.add(features.get(i));
            }
        }

        return result;
    }

    public void setAllSelected(boolean isSelected) {
        for (int i=0; i<checkedStatus.length; i++) {
            checkedStatus[i] = isSelected;
        }

        notifyDataSetChanged();
    }

    static class BuildingViewHolder extends RecyclerView.ViewHolder {
        final Button button;
        final CheckBox checkbox;
        final TextView floor;
        final TextView description;
        final TextView id;
        final View divider;
        BuildingViewHolder(View v) {
            super(v);
            button = (Button) v.findViewById(R.id.feature_title);
            checkbox = (CheckBox) v.findViewById(R.id.feature_checkbox);
            floor = (TextView) v.findViewById(R.id.feature_floor);
            description = (TextView) v.findViewById(R.id.feature_description);
            id = (TextView) v.findViewById(R.id.feature_id);
            divider = v.findViewById(R.id.feature_divider);
        }
    }
}
