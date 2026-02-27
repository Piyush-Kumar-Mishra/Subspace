package com.example.linkit.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenStore(private val dataStore : DataStore<Preferences>) {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val USER_ID_KEY = longPreferencesKey("user_id")
    private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
    private val PROFILE_COMPLETED = booleanPreferencesKey("profile_completed")

    val token: Flow<String?> = dataStore.data.map { it[TOKEN_KEY] }
    val userId: Flow<Long?> = dataStore.data.map { it[USER_ID_KEY] }


    suspend fun saveToken(token: String) {
        dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun clearToken() {
        dataStore.edit { it.clear() }
    }

    suspend fun saveUserId(id: Long) {
        dataStore.edit { it[USER_ID_KEY] = id }
    }

    suspend fun getFcmTokenImmediate(): String? {
        return dataStore.data.first()[FCM_TOKEN_KEY]
    }

    suspend fun saveFcmToken(token: String) {
        dataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = token
        }
    }

    suspend fun saveProfileStatus(isCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PROFILE_COMPLETED] = isCompleted
        }
    }

    val isProfileCompleted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PROFILE_COMPLETED] ?: false
        }

}
