package com.guoyuan.memoria

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.BillingClientStateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PremiumManager(private val context: Context, private val dataStore: DataStore<Preferences>) {
    private val PREMIUM_KEY = booleanPreferencesKey("is_premium")
    
    val isPremium: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PREMIUM_KEY] ?: false
        }

    suspend fun setPremium(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREMIUM_KEY] = value
        }
    }
    // 1. 初始化 BillingClient
    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases -> /* 購買更新回調 */ }
        .enablePendingPurchases()
        .build()

    // 2. 核心查詢函式：整合「自動檢查」與「手動恢復」
    fun refreshPurchaseStatus() {
        billingClient.startConnection(object : BillingClientStateListener {
            // 必須使用 override 關鍵字
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()

                    billingClient.queryPurchasesAsync(params) { _, purchases ->
                        val isPurchased = purchases.any { it.products.contains("你的產品ID") }

                        // 修正：CoroutineScope 需要一個 Scope，如果是在類別內，建議從 ViewModel 傳入
                        // 或者直接使用 GlobalScope (測試用)
                        CoroutineScope(Dispatchers.IO).launch {
                            setPremium(isPurchased)
                        }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // 這裡可以寫重連邏輯
            }
        })
    }
}
