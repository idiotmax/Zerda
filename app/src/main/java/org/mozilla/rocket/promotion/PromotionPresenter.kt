package org.mozilla.rocket.promotion


import android.content.Context
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.Settings
import kotlin.properties.Delegates

interface PromotionViewContract {
    fun postSurveyNotification()
    fun showRateAppDialog()
    fun showRateAppNotification()
    fun showShareAppDialog()
    fun showPrivacyPolicyUpdateNotification()
    fun showRateAppDialogFromIntent()
    fun isPromotionFromIntent(safeIntent: SafeIntent): Boolean
}

class PromotionModel(context: Context, fromIntent: Boolean) {

    // using a notnull delegate will make sure if the value is not set, it'll throw exception
    var didShowRateDialog by Delegates.notNull<Boolean>()
    var didShowShareDialog by Delegates.notNull<Boolean>()
    var isSurveyEnabled by Delegates.notNull<Boolean>()
    var didShowRateAppNotification by Delegates.notNull<Boolean>()
    var didDismissRateDialog by Delegates.notNull<Boolean>()
    var appCreateCount by Delegates.notNull<Int>()

    var rateAppDialogThreshold by Delegates.notNull<Long>()
    var rateAppNotificationThreshold by Delegates.notNull<Long>()
    var shareAppDialogThreshold by Delegates.notNull<Long>()


    var shouldShowPrivacyPolicyUpdate by Delegates.notNull<Boolean>()

    var showRateAppDialogFromIntent by Delegates.notNull<Boolean>()

    init {

        val history = Settings.getInstance(context).eventHistory
        didShowRateDialog = history.contains(Settings.Event.ShowRateAppDialog)
        didShowShareDialog = history.contains(Settings.Event.ShowShareAppDialog)
        didDismissRateDialog = history.contains(Settings.Event.DismissRateAppDialog)
        didShowRateAppNotification = history.contains(Settings.Event.ShowRateAppNotification)
        isSurveyEnabled = AppConfigWrapper.isSurveyNotificationEnabled() && !history.contains(Settings.Event.PostSurveyNotification)
        if (accumulateAppCreateCount()) {
            history.add(Settings.Event.AppCreate)
        }
        appCreateCount = history.getCount(Settings.Event.AppCreate)
        rateAppDialogThreshold = AppConfigWrapper.getRateDialogLaunchTimeThreshold(context)
        rateAppNotificationThreshold = AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold(context)
        shareAppDialogThreshold = AppConfigWrapper.getShareDialogLaunchTimeThreshold(context, didDismissRateDialog)

        shouldShowPrivacyPolicyUpdate = NewFeatureNotice.getInstance(context).shouldShowPrivacyPolicyUpdate()
        showRateAppDialogFromIntent = fromIntent

    }


    private fun accumulateAppCreateCount() = !didShowRateDialog || !didShowShareDialog || isSurveyEnabled || !didShowRateAppNotification

}

class PromotionPresenter {
    companion object {

        @JvmStatic
        fun runPromotion(promotionViewContract: PromotionViewContract, promotionModel: PromotionModel) {
            if (runPromotionFromIntent(promotionViewContract, promotionModel)) {
                // Don't run other promotion if we already displayed above promotion
                return
            }

            if (!promotionModel.didShowRateDialog && promotionModel.appCreateCount >= promotionModel.rateAppDialogThreshold) {
                promotionViewContract.showRateAppDialog()

            } else if (promotionModel.didDismissRateDialog && !promotionModel.didShowRateAppNotification && promotionModel.appCreateCount >= promotionModel.rateAppNotificationThreshold) {
                promotionViewContract.showRateAppNotification()

            } else if (!promotionModel.didShowShareDialog && promotionModel.appCreateCount >= promotionModel.shareAppDialogThreshold) {
                promotionViewContract.showShareAppDialog()

            }

            if (promotionModel.isSurveyEnabled && promotionModel.appCreateCount >= AppConfigWrapper.getSurveyNotificationLaunchTimeThreshold()) {
                promotionViewContract.postSurveyNotification()
            }

            if (promotionModel.shouldShowPrivacyPolicyUpdate) {
                promotionViewContract.showPrivacyPolicyUpdateNotification()
            }
        }

        @JvmStatic
        // return true if promotion is already handled
        fun runPromotionFromIntent(promotionViewContract: PromotionViewContract, promotionModel: PromotionModel): Boolean {
            // When we receive this action, it means we need to show "Love Rocket" dialog
            if (promotionModel.showRateAppDialogFromIntent) {
                promotionViewContract.showRateAppDialogFromIntent()
                return true
            }
            return false
        }
    }


}