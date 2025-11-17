package com.example.linkit.data.models.auth_models

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String
)