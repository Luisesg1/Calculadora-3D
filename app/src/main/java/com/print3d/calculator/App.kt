package com.print3d.calculator

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.print3d.calculator.data.notify.StockNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var stockNotifier: StockNotifier

    override fun onCreate() {
        super.onCreate()
        // Safe to call on any thread; the SDK does its heavy init off the main thread.
        MobileAds.initialize(this) {}
        stockNotifier.ensureChannel()
    }
}
