package com.print3d.calculator.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one full-screen interstitial ready and shows it on demand. Preloads eagerly and
 * again after every show so the next one is ready. Fails silently if no ad is loaded — an
 * interstitial is opportunistic, never a blocker.
 *
 * AD_UNIT is Google's official TEST interstitial id — swap for your real AdMob unit before release.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private var ad: InterstitialAd? = null
    private var loading = false

    fun preload() {
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            appContext,
            AD_UNIT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) { ad = loaded; loading = false }
                override fun onAdFailedToLoad(error: LoadAdError) { ad = null; loading = false }
            }
        )
    }

    /** Show the ad if one is ready; otherwise just preload for next time. */
    fun show(activity: Activity) {
        val current = ad ?: run { preload(); return }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { ad = null; preload() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { ad = null; preload() }
        }
        current.show(activity)
    }

    companion object {
        // TEST unit in debug, real unit in release — wired via buildConfigField (build.gradle.kts).
        val AD_UNIT: String = com.print3d.calculator.BuildConfig.ADMOB_INTERSTITIAL
    }
}
