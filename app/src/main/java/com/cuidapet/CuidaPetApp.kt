package com.cuidadopet

import android.app.Application
import com.cuidadopet.notification.NotificationChannels
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CuidadoPetApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        // MobileAds.initialize(this)
    }
}
