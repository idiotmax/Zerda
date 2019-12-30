package org.mozilla.rocket.periodic

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.notification.FirebaseMessagingServiceWrapper
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.IntentUtils

class FirstLaunchWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        val TAG: String = FirstLaunchWorker::class.java.simpleName
        val ACTION: String = BuildConfig.APPLICATION_ID + ".action." + TAG

        const val TIMER_DISABLED = 0
        const val TIMER_SUSPEND = -1

        private const val PREF_KEY_BOOLEAN_NOTIFICATION_FIRED: String = "pref-key-boolean-notification-fired"

        private const val REQUEST_CODE_CLICK_NOTIFICATION = 1
        private const val REQUEST_CODE_DELETE_NOTIFICATION = 2

        fun isNotificationFired(context: Context, default: Boolean = false): Boolean {
            return getSharedPreference(context).getBoolean(PREF_KEY_BOOLEAN_NOTIFICATION_FIRED, default)
        }

        fun setNotificationFired(context: Context, value: Boolean) {
            val edit = getSharedPreference(context).edit()
            edit.putBoolean(PREF_KEY_BOOLEAN_NOTIFICATION_FIRED, value)
            edit.apply()
        }

        private fun getSharedPreference(context: Context): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }
    }

    override fun doWork(): Result {
        val firstrunNotification = AppConfigWrapper.getFirstLaunchNotification().jsonStringToFirstrunNotification()
        firstrunNotification?.run {
            showNotification(applicationContext, message, openUrl, command, deepLink)
            TelemetryWrapper.showFirstrunNotification(AppConfigWrapper.getFirstLaunchWorkerTimer(), message, openUrl ?: command ?: deepLink)
            setNotificationFired(applicationContext, true)
        }

        return Result.success()
    }

    private fun showNotification(context: Context, message: String, openUrl: String?, command: String?, deepLink: String?) {
        val intent = IntentUtils.genFirstrunNotificationClickForBroadcastReceiver(context, message, openUrl, command, deepLink)
        val openRocketPending = PendingIntent.getBroadcast(context, REQUEST_CODE_CLICK_NOTIFICATION, intent,
                PendingIntent.FLAG_ONE_SHOT)
        val builder = NotificationUtil.importantBuilder(context)
                .setContentTitle(message)
                .setContentIntent(openRocketPending)

        addDeleteTelemetry(applicationContext, builder, message, openUrl ?: command ?: deepLink)
        // Show notification
        val id = message.hashCode()
        NotificationUtil.sendNotification(context, id, builder)
    }

    private fun addDeleteTelemetry(appContext: Context, builder: NotificationCompat.Builder, message: String, link: String?) {
        val intent = IntentUtils.genDeleteFirstrunNotificationActionForBroadcastReceiver(appContext, message, link)
        val pendingIntent = PendingIntent.getBroadcast(appContext, REQUEST_CODE_DELETE_NOTIFICATION, intent, PendingIntent.FLAG_ONE_SHOT)
        builder.setDeleteIntent(pendingIntent)
    }
}

class FirstrunNotification(
    val message: String,
    val openUrl: String?,
    val command: String?,
    val deepLink: String?
)

fun String.jsonStringToFirstrunNotification(): FirstrunNotification? {
    return try {
        val jsonObject = JSONObject(this)
        FirstrunNotification(
            jsonObject.getString("message"),
            jsonObject.optString(FirebaseMessagingServiceWrapper.PUSH_OPEN_URL, null),
            jsonObject.optString(FirebaseMessagingServiceWrapper.PUSH_COMMAND, null),
            jsonObject.optString(FirebaseMessagingServiceWrapper.PUSH_DEEP_LINK, null)
        )
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}