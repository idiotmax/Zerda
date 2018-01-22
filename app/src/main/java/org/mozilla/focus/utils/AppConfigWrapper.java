/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

public class AppConfigWrapper {
    private static final int SURVEY_NOTIFICATION_POST_THRESHOLD = 3;

    public static int getShareDialogLaunchTimeThreshold() {
        return DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_APP;
    }

    public static int getRateDialogLaunchTimeThreshold() {
        return DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_APP;
    }

    public static int getSurveyNotificationLaunchTimeThreshold() {
        return SURVEY_NOTIFICATION_POST_THRESHOLD;
    }
}
