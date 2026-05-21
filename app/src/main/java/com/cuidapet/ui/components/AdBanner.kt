package com.cuidadopet.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.viewinterop.AndroidView
import com.cuidadopet.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlin.random.Random

private val promoMessages = listOf(
    "Os anúncios tornam o CuidadoPet possível 🐾",
    "Esse anúncio ajuda a manter o app gratuito. Obrigada! 💙",
    "Cada anúncio visto ajuda o CuidadoPet a crescer 🌱"
)

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val viewModel: AdBannerViewModel = hiltViewModel()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    if (isPremium) return

    val adMobActive = BuildConfig.BANNER_AD_UNIT_ID.isNotBlank()
    val showAdMob   = remember { adMobActive && Random.nextFloat() < 0.7f }
    val message     = remember { promoMessages.random() }

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
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}
