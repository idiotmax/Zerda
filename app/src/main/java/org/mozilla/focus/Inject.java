/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus;

import android.content.Context;
import android.preference.PreferenceManager;

import org.mozilla.focus.home.HomeFragment;

public class Inject {

    public static String getDefaultTopSites(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(HomeFragment.TOPSITES_PREF, null);

    }
}
