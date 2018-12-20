/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.history.BrowsingHistoryManager
import org.mozilla.focus.locale.LocaleAwareApplication
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.screenshot.ScreenshotManager
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AdjustHelper
import org.mozilla.rocket.partner.PartnerActivator
import org.mozilla.rocket.privately.PrivateModeContextWrapper

class FocusApplication : LocaleAwareApplication() {

    lateinit var partnerActivator: PartnerActivator
    lateinit var base: Context
    lateinit var privateWrappedContext: PrivateModeContextWrapper

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        // Provide different strict mode penalty for ui testing and production code
        Inject.enableStrictMode()

        SearchEngineManager.getInstance().init(this)

        TelemetryWrapper.init(this)
        AdjustHelper.setupAdjustIfNeeded(this)

        BrowsingHistoryManager.getInstance().init(this)
        ScreenshotManager.getInstance().init(this)
        DownloadInfoManager.getInstance()
        DownloadInfoManager.init(this)
        // initialize the NotificationUtil to configure the default notification channel. This is required for API 26+
        NotificationUtil.init(this)

        partnerActivator = PartnerActivator(this)
        partnerActivator.launch()

        privateWrappedContext.inject(this)
    }

    override fun getBaseContext(): Context {
        return base
    }

    override fun attachBaseContext(baseContext: Context?) {
        base = baseContext!!
        privateWrappedContext = PrivateModeContextWrapper(base)
        super.attachBaseContext(privateWrappedContext)
    }
}
