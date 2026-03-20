package com.example.linkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linkit.data.TokenStore
import com.example.linkit.data.repo.NotificationRepository
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.UiEvent
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                println("Fetching FCM registration token failed: ${task.exception}")
                return@addOnCompleteListener
            }

            val newToken = task.result

            viewModelScope.launch {
                val savedToken = tokenStore.getFcmTokenImmediate()

                if (newToken != null && newToken != savedToken) {
                    println("DEBUG: Token changed. New: $newToken")
                    registerToken(newToken)
                } else {
                    println("DEBUG: FCM Token matches saved token. Skipping API call.")
                }
            }
        }
    }

    private fun registerToken(token: String) {
        println("DEBUG: Attempting to register FCM Token: $token")

        viewModelScope.launch {
            repository.registerToken(token).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        println("Token registered successfully")
                        tokenStore.saveFcmToken(token)
                    }
                    is NetworkResult.Error -> {
                        println("Failed to register token: ${result.message}")
                    }
                    else -> Unit
                }
            }
        }
    }
}