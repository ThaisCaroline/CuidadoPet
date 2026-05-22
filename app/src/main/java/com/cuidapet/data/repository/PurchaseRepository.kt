package com.cuidadopet.data.repository

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepository @Inject constructor() {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        runCatching {
            Purchases.sharedInstance.updatedCustomerInfoListener =
                UpdatedCustomerInfoListener { info -> _isPremium.value = info.isPremium }

            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isPremium.value = customerInfo.isPremium
                }
                override fun onError(error: PurchasesError) {}
            })
        }
    }

    fun getOfferings(onResult: (Offerings?) -> Unit) {
        runCatching {
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) = onResult(offerings)
                override fun onError(error: PurchasesError)  = onResult(null)
            })
        }.onFailure { onResult(null) }
    }

    fun purchase(activity: Activity, rcPackage: Package, onResult: (Boolean) -> Unit) {
        runCatching {
            Purchases.sharedInstance.purchase(
                PurchaseParams.Builder(activity, rcPackage).build(),
                object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        _isPremium.value = customerInfo.isPremium
                        onResult(true)
                    }
                    override fun onError(error: PurchasesError, userCancelled: Boolean) = onResult(false)
                }
            )
        }.onFailure { onResult(false) }
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) {
        runCatching {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isPremium.value = customerInfo.isPremium
                    onResult(true)
                }
                override fun onError(error: PurchasesError) = onResult(false)
            })
        }.onFailure { onResult(false) }
    }

    private val CustomerInfo.isPremium: Boolean
        get() = entitlements["premium"]?.isActive == true
}
