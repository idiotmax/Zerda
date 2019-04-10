/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;

import android.support.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.rocket.content.data.ShoppingLink;
import org.mozilla.rocket.content.data.ShoppingLinkKey;

import java.util.ArrayList;

public class AppConfigWrapper {
    static final int SURVEY_NOTIFICATION_POST_THRESHOLD = 3;
    static final boolean PRIVATE_MODE_ENABLED_DEFAULT = true;
    static final boolean LIFE_FEED_ENABLED_DEFAULT = false;
    static final String LIFE_FEED_PROVIDERS_DEFAULT = "";
    static final String STR_E_COMMERCE_SHOPPINGLINKS_DEFAULT = "";


    /* Disabled since v1.0.4, keep related code in case we want to enable it again in the future */
    private static final boolean SURVEY_NOTIFICATION_ENABLED = false;
    static final int DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD = 2;

    public static long getRateAppNotificationLaunchTimeThreshold(Context context) {
        return FirebaseHelper.getRcLong(context, FirebaseHelper.RATE_APP_NOTIFICATION_THRESHOLD);
    }

    public static long getShareDialogLaunchTimeThreshold(Context context, final boolean needExtend) {
        if (needExtend) {
            return FirebaseHelper.getRcLong(context, FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD) +
                    getRateAppNotificationLaunchTimeThreshold(context) -
                    getRateDialogLaunchTimeThreshold(context);
        }
        return FirebaseHelper.getRcLong(context, FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD);
    }

    public static long getRateDialogLaunchTimeThreshold(Context context) {
        return FirebaseHelper.getRcLong(context, FirebaseHelper.RATE_APP_DIALOG_THRESHOLD);
    }

    public static int getSurveyNotificationLaunchTimeThreshold() {
        return SURVEY_NOTIFICATION_POST_THRESHOLD;
    }

    public static int getDriveDefaultBrowserFromMenuSettingThreshold() {
        return DRIVE_DEFAULT_BROWSER_FROM_MENU_SETTING_THRESHOLD;
    }

    public static boolean isPrivateModeEnabled(Context context) {
        return FirebaseHelper.getRcBoolean(context, FirebaseHelper.ENABLE_PRIVATE_MODE);
    }

    public static boolean getMyshotUnreadEnabled(Context context) {
        return FirebaseHelper.getRcBoolean(context, FirebaseHelper.ENABLE_MY_SHOT_UNREAD);
    }

    public static boolean isSurveyNotificationEnabled() {
        return SURVEY_NOTIFICATION_ENABLED;
    }

    public static String getRateAppDialogTitle(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE);
    }

    public static String getRateAppDialogContent(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT);
    }

    public static String getRateAppPositiveString(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.RATE_APP_DIALOG_TEXT_POSITIVE);
    }

    public static String getRateAppNegativeString(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.RATE_APP_DIALOG_TEXT_NEGATIVE);
    }

    public static String getBannerRootConfig(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.BANNER_MANIFEST);
    }

    public static long getFeatureSurvey(Context context) {
        return FirebaseHelper.getRcLong(context, FirebaseHelper.FEATURE_SURVEY);
    }

    public static String getScreenshotCategoryUrl(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.SCREENSHOT_CATEGORY_MANIFEST);
    }

    public static String getVpnRecommenderUrl(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.VPN_RECOMMENDER_URL);
    }

    public static String getVpnRecommenderPackage(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.VPN_RECOMMENDER_PACKAGE);
    }

    public static long getFirstLaunchWorkerTimer(Context context) {
        return FirebaseHelper.getRcLong(context, FirebaseHelper.FIRST_LAUNCH_TIMER_MINUTES);
    }

    public static String getFirstLaunchNotificationMessage(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.FIRST_LAUNCH_NOTIFICATION_MESSAGE);
    }

    public static boolean isLifeFeedEnabled(Context context) {
        return FirebaseHelper.getRcBoolean(context, FirebaseHelper.ENABLE_LIFE_FEED);
    }


    /**
     * Return a list of vouchers and shopping links for e-commerce content portal.
     * This is also used to determine if the user should see e-commerce or News in content portal.
     * In the future, the user may have both e-commerce and News. But now, let's make it simple.
     * @return ArrayList of shopping links or empty list if we encounter an error.
     */
    public static ArrayList<ShoppingLink> getEcommerceShoppingLinks() {
        ArrayList<ShoppingLink> shoppingLinks = new ArrayList<>();

        final String rcString = FirebaseHelper.getRcString(null, FirebaseHelper.STR_E_COMMERCE_SHOPPINGLINKS);
        try {
            final JSONArray jsonArray = new JSONArray(rcString);
            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject object = (JSONObject) jsonArray.get(i);
                shoppingLinks.add(new ShoppingLink(
                        object.optString(ShoppingLinkKey.KEY_URL),
                        object.optString(ShoppingLinkKey.KEY_NAME),
                        object.optString(ShoppingLinkKey.KEY_IMAGE),
                        object.optString(ShoppingLinkKey.KEY_SOURCE)));
            }

        } catch (JSONException e) {
            // skip and do nothing
        }

        return shoppingLinks;
    }

    public static String getLifeFeedProviderUrl(Context context, String provider) {
        String source = FirebaseHelper.getRcString(context, FirebaseHelper.LIFE_FEED_PROVIDERS);
        String url = "";

        try {
            JSONArray rows = new JSONArray(source);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                if (row.getString("name").equalsIgnoreCase(provider)) {
                    url = row.getString("url");
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return url;
    }

    static String getShareAppDialogTitle(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.STR_SHARE_APP_DIALOG_TITLE);
    }

    // Only this field supports prettify
    static String getShareAppDialogContent(Context context) {
        final String rcString = FirebaseHelper.getRcString(context, FirebaseHelper.STR_SHARE_APP_DIALOG_CONTENT);
        return FirebaseHelper.prettify(rcString);
    }

    static String getShareAppMessage(Context context) {
        return FirebaseHelper.getRcString(context, FirebaseHelper.STR_SHARE_APP_DIALOG_MSG);
    }
}
