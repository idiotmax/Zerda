package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.focus.activity.SettingsActivity

class StartSettingsActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(SettingsActivity.getStartIntent(context))
    }
}
