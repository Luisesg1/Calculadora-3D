package com.print3d.calculator

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Safe to call on any thread; the SDK does its heavy init off the main thread.
        MobileAds.initialize(this) {}
    }
}
