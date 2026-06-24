package com.guoyuan.memoria

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PremiumManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val externalScope: CoroutineScope // 傳入 ViewModel 的 scope
) {

    // 讓 ViewModel 檢查連線狀態
    val isBillingReady: Boolean
        get() = billingClient.isReady
    private val PREMIUM_KEY = booleanPreferencesKey("is_premium")
    private val PRODUCT_ID = "memoria_remove_ads_permanent"

    // 狀態存取
    // 直接將 DataStore 的 Flow 轉換為 UI 可以觀察的 StateFlow
    val isPremium: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[PREMIUM_KEY] ?: false }
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly, // 確保 App 一啟動就開始同步
            initialValue = false
        )

    // 購買監聽器獨立出來，避免重複初始化
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d("BillingManager", "使用者取消購買")
        } else {
            Log.e("BillingManager", "購買失敗: ${billingResult.debugMessage}")
        }
    }
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()
    // 封裝連線邏輯
    fun startBillingConnection(onFinished: (Boolean) -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                onFinished(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            }
            override fun onBillingServiceDisconnected() {
                onFinished(false)
            }
        })
    }


    fun refreshPurchaseStatus() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // 查詢用戶已擁有的商品
                    val params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()

                    billingClient.queryPurchasesAsync(params) { _, purchases ->
                        handlePurchases(purchases)
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                // 簡單的延遲重試，或者直接再次呼叫連線
                //refreshPurchaseStatus()
            }
        })
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        // 1. 先根據你的邏輯更新 DataStore (這是為了讓 UI 知道已擁有購買權)
        val isPurchased = purchases.any {
            it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        externalScope.launch {
            dataStore.edit { preferences ->
                preferences[PREMIUM_KEY] = isPurchased
            }
        }

        // 2. 對每一個 purchase 進行確認交易 (Acknowledge)
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("BillingManager", "購買已成功確認！")
                    }
                }
            }
        }
    }

    // 記得提供一個關閉連線的方法
    fun dispose() {
        billingClient.endConnection()
    }
    // 新增：觸發購買的方法
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        Log.d("BillingManager", "launchPurchaseFlow result: $responseCode")
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e("BillingManager", "購買發起失敗，錯誤代碼: $responseCode")
        }
    }

    // 新增：取得商品詳細資訊的方法 (UI 顯示價格用)
    fun getProductDetails(productId: String, callback: (ProductDetails?) -> Unit) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ))
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
        // 檢查這裡是否有正確接收到 billingResult
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList?.firstOrNull()
                if (details != null) {
                    // 成功獲取商品
                    Log.d("BillingManager", "商品資訊已獲取: ${details.title}")
                    callback(details)
                } else {
                    Log.e("BillingManager", "找不到商品")
                    callback(null)
                }
            } else {
                Log.e("BillingManager", "查詢商品資訊失敗: ${billingResult.debugMessage}")
                callback(null)
            }
        }
    }

    // 當購買成功時，只需寫入 DataStore，StateFlow 會自動收到通知更新
    private suspend fun updatePremiumStatus(isPurchased: Boolean) {
        setPremium(isPurchased)
    }
    suspend fun setPremium(value: Boolean) {

        dataStore.edit { preferences ->

            preferences[PREMIUM_KEY] = value

        }

    }

}
