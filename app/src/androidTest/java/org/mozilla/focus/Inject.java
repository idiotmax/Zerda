/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus;

import android.content.ContentResolver;
import android.content.Context;

import org.mozilla.focus.provider.MockQueryHandler;
import org.mozilla.focus.provider.QueryHandler;

public class Inject {

    private static String TOP_SITES = "[{\"id\":-1,\"url\":\"https:\\/\\/m.youtube.com\\/\",\"title\":\"Youtube\",\"favicon\":\"ic_youtube.png\",\"viewCount\":20,\"lastViewTimestamp\":1517196818119},{\"id\":-3,\"url\":\"http:\\/\\/m.tribunnews.com\\/\",\"title\":\"Tribunnews\",\"favicon\":\"ic_tribunnews.png\",\"viewCount\":18,\"lastViewTimestamp\":1517196818119},{\"id\":-5,\"url\":\"https:\\/\\/m.tokopedia.com\\/\",\"title\":\"Tokopedia\",\"favicon\":\"ic_tokopedia.png\",\"viewCount\":16,\"lastViewTimestamp\":1517196818119},{\"id\":-4,\"url\":\"https:\\/\\/m.facebook.com\\/\",\"title\":\"Facebook\",\"favicon\":\"ic_facebook.png\",\"viewCount\":14,\"lastViewTimestamp\":1517196818119},{\"id\":-8,\"url\":\"https:\\/\\/m.bukalapak.com\\/\",\"title\":\"Bukalapak\",\"favicon\":\"ic_bukalapak.png\",\"viewCount\":12,\"lastViewTimestamp\":1517196818119},{\"id\":-6,\"url\":\"http:\\/\\/m.liputan6.com\\/\",\"title\":\"Liputan6\",\"favicon\":\"ic_liputan6.png\",\"viewCount\":10,\"lastViewTimestamp\":1517196818119},{\"id\":-7,\"url\":\"http:\\/\\/www.kompas.com\\/\",\"title\":\"Kompas\",\"favicon\":\"ic_kompas.png\",\"viewCount\":8,\"lastViewTimestamp\":1517196818119},{\"id\":-9,\"url\":\"https:\\/\\/m.kapanlagi.com\\/\",\"title\":\"Kapanlagi\",\"favicon\":\"ic_kapanlagi.png\",\"viewCount\":6,\"lastViewTimestamp\":1517196818119}]";

    public static QueryHandler getQueryHandler(ContentResolver resolver) {
        return new MockQueryHandler(resolver);
    }

    public static String getDefaultTopSites(Context context) {
        return TOP_SITES;
    }
}
