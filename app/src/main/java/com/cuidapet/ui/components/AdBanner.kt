package com.cuidadopet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.BuildConfig
import com.cuidadopet.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlin.random.Random

private val promoImages = listOf(
    R.drawable.footer1,
    R.drawable.footer2,
    R.drawable.footer3
)

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val viewModel: AdBannerViewModel = hiltViewModel()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    if (isPremium) return

    val adMobActive = BuildConfig.BANNER_AD_UNIT_ID.isNotBlank()
    val showAdMob   = remember { adMobActive && Random.nextFloat() < 0.8f }
    val image       = remember { promoImages.random() }

    if (showAdMob) {
        AndroidView(
            modifier = modifier.fillMaxWidth().padding(top = 16.dp),
            factory  = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BuildConfig.BANNER_AD_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    } else {
        Image(
            painter            = painterResource(image),
            contentDescription = null,
            modifier           = modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentScale       = ContentScale.FillWidth
        )
    }
}
