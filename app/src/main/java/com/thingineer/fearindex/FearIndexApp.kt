package com.thingineer.fearindex

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FearIndexApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupTimber()
        setupNotificationChannels()
        setupAdMob()
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun setupNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            "fear_index_alerts",
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun setupAdMob() {
        MobileAds.initialize(this)
    }
}
