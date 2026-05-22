package com.cuidadopet.ui.screens.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.repository.PurchaseRepository
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallUiState(
    val isLoading: Boolean = true,
    val offerings: Offerings? = null,
    val isPremium: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            purchaseRepository.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
        purchaseRepository.getOfferings { offerings ->
            _uiState.update { it.copy(isLoading = false, offerings = offerings) }
        }
    }

    fun purchase(activity: Activity, rcPackage: Package) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        purchaseRepository.purchase(activity, rcPackage) { success ->
            _uiState.update {
                it.copy(
                    isLoading    = false,
                    errorMessage = if (!success) "Compra não concluída. Tente novamente." else null
                )
            }
        }
    }

    fun restorePurchases(activity: Activity) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        purchaseRepository.restorePurchases { success ->
            _uiState.update {
                it.copy(
                    isLoading    = false,
                    errorMessage = if (!success) "Nenhuma compra encontrada." else null
                )
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
