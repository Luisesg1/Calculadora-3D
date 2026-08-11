package com.print3d.calculator.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the Google Play Billing connection and exposes a single source of truth:
 * [isSubscribed]. Subscription state is always re-derived from Play's own purchase
 * records — never persisted locally as authority — so it can't be spoofed.
 *
 * NOTE: real entitlement only resolves for a build installed from Play (internal testing
 * track or higher) with a matching subscription product configured in Play Console. In a
 * plain debug build the client connects but reports no purchases, so ads stay on.
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext context: Context
) : PurchasesUpdatedListener {

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init { connect() }

    private fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
            }
            override fun onBillingServiceDisconnected() { /* Play will reconnect on next query. */ }
        })
    }

    /** Re-read active purchases from Play and update [isSubscribed]. */
    fun refresh() {
        if (!client.isReady) { connect(); return }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { _, purchases ->
            val active = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED && it.products.contains(PRODUCT_ID)
            }
            _isSubscribed.value = active
            purchases.forEach(::acknowledge)
        }
    }

    /**
     * Launch Play's subscribe sheet for [PRODUCT_ID]. [basePlanId] picks monthly vs annual
     * ([BASE_PLAN_MONTHLY]/[BASE_PLAN_ANNUAL]); null falls back to the first available plan.
     */
    fun launchSubscribe(activity: Activity, basePlanId: String? = null) {
        if (!client.isReady) { connect(); return }
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        client.queryProductDetailsAsync(params) { _, productDetailsList ->
            val details = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val offers = details.subscriptionOfferDetails
            val offerToken = (offers?.firstOrNull { it.basePlanId == basePlanId } ?: offers?.firstOrNull())
                ?.offerToken ?: return@queryProductDetailsAsync
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()
            client.launchBillingFlow(activity, flowParams)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach(::acknowledge)
            refresh()
        }
    }

    private fun acknowledge(p: Purchase) {
        if (p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(p.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { }
        }
    }

    companion object {
        /** Must match the subscription product id you create in Play Console. */
        const val PRODUCT_ID = "remove_ads"
        /** Base plan ids inside that product — must match Play Console exactly. */
        const val BASE_PLAN_MONTHLY = "monthly"
        const val BASE_PLAN_ANNUAL = "annual"
    }
}
