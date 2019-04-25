package org.mozilla.banner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import org.json.JSONException;

class BasicViewHolder extends BannerViewHolder {
    static final int VIEW_TYPE = 0;
    static final String VIEW_TYPE_NAME = "basic";
    private ImageView background;
    private OnClickListener onClickListener;

    BasicViewHolder(ViewGroup parent, OnClickListener onClickListener, TelemetryListener telemetryListener) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.banner0, parent, false), telemetryListener);
        this.onClickListener = onClickListener;
        background = itemView.findViewById(R.id.banner_background);
    }

    @Override
    public void onBindViewHolder(Context context, BannerDAO bannerDAO) {
        super.onBindViewHolder(context, bannerDAO);
        try {
            Glide.with(context).load(bannerDAO.values.getString(0)).into(background);
        } catch (JSONException e) {
            // Invalid manifest
            e.printStackTrace();
        }
        background.setOnClickListener(v -> {
            sendClickBackgroundTelemetry();
            try {
                onClickListener.onClick(bannerDAO.values.getString(1));
            } catch (JSONException e) {
                // Invalid manifest
                e.printStackTrace();
            }
        });
    }
}
