/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;

class SiteViewHolder extends RecyclerView.ViewHolder {

    FrameLayout imgContainer;
    ImageView img;
    TextView text;

    public SiteViewHolder(View itemView) {
        super(itemView);
        imgContainer = (FrameLayout) itemView.findViewById(R.id.content_image_background);
        img = (ImageView) itemView.findViewById(R.id.content_image);
        text = (TextView) itemView.findViewById(R.id.text);
    }
}
