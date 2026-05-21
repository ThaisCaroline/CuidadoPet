package com.cuidadopet.ui.components

import androidx.lifecycle.ViewModel
import com.cuidadopet.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AdBannerViewModel @Inject constructor(
    purchaseRepository: PurchaseRepository
) : ViewModel() {
    val isPremium = purchaseRepository.isPremium
}
