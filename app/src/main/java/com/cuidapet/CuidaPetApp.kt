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
        MobileAds.initialize(this)
        // PAUSADO: inicialização do RevenueCat comentada (cobrança desligada)
        // if (BuildConfig.REVENUECAT_API_KEY.isNotBlank()) {
        //     Purchases.configure(PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build())
        // }
    }
}
