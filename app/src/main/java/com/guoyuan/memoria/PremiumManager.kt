package com.guoyuan.memoria

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PremiumManager(private val dataStore: DataStore<Preferences>) {
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
}
