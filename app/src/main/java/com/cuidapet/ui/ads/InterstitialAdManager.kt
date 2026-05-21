package com.cuidadopet.ui.ads

import android.app.Activity
import android.content.Context
import com.cuidadopet.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference

class InterstitialAdManager(context: Context) {

    private val appContext    = context.applicationContext
    private var interstitialAd: InterstitialAd? = null
    private var pendingActivity: WeakReference<Activity>? = null

    init {
        load()
    }

    private fun load() {
        if (BuildConfig.INTERSTITIAL_AD_UNIT_ID.isBlank()) return
        InterstitialAd.load(
            appContext,
            BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent()              { load() }
                        override fun onAdFailedToShowFullScreenContent(e: AdError) { load() }
                    }
                    // Mostra imediatamente se show() foi chamado antes de carregar
                    pendingActivity?.get()?.let { interstitialAd?.show(it) }
                    pendingActivity = null
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd  = null
                    pendingActivity = null
                }
            }
        )
    }

    fun show(activity: Activity) {
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
        } else {
            pendingActivity = WeakReference(activity)
        }
    }
}
