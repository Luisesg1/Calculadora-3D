package com.print3d.calculator.feature.monetization

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModel
import com.print3d.calculator.data.ads.InterstitialAdManager
import com.print3d.calculator.data.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * All monetization decisions in one place: subscription state + when to show an interstitial.
 * Non-subscribers see a full-screen ad every [SCREENS_PER_AD] screen views; subscribers never do.
 */
@HiltViewModel
class MonetizationViewModel @Inject constructor(
    private val billing: BillingRepository,
    private val interstitial: InterstitialAdManager
) : ViewModel() {

    /** True while the user has an active "remove ads" subscription. */
    val isSubscribed: StateFlow<Boolean> = billing.isSubscribed

    private var screenCount = 0

    init { interstitial.preload() }

    /** Re-check Play for entitlement (call after returning from the purchase flow). */
    fun refresh() = billing.refresh()

    fun subscribe(activity: Activity, basePlanId: String? = null) = billing.launchSubscribe(activity, basePlanId)

    /**
     * Call on every screen view. Counts views and fires an interstitial once the threshold is
     * reached. No-op for subscribers. Must be driven from the single Activity-scoped instance
     * (AppNavGraph) so the counter is global, not per-screen.
     */
    fun notifyScreenView(activity: Activity) {
        if (isSubscribed.value) return
        screenCount++
        if (screenCount >= SCREENS_PER_AD) {
            screenCount = 0
            interstitial.show(activity)
        }
    }

    companion object {
        /** Show a full-screen ad once every this many screen views. */
        const val SCREENS_PER_AD = 5

        /** Free users can save at most this many quotes; Pro is unlimited. */
        const val FREE_QUOTE_LIMIT = 10

        /** Free users can save at most this many templates; Pro is unlimited. */
        const val FREE_TEMPLATE_LIMIT = 1
    }
}

/** Unwrap a Compose [Context] (often a ContextWrapper) to the hosting Activity. */
fun findActivity(context: Context): Activity? {
    var c: Context = context
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
