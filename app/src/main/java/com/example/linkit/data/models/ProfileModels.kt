package com.example.linkit.data.models

data class CreateProfileRequest(
    val name :String,
    val jobTitle :String?=null,
    val company : String? = null,
    val aboutMe :String? = null,
    val profileImageBase64: String? = null
)

data class UpdateProfileRequest(
    val name: String,
    val jobTitle: String? =null,
    val company: String? = null,
    val aboutMe: String? = null,
    val profileImageBase64: String? = null
)

data class ProfileResponse(
    val userId: Long,
    val name: String,
    val jobTitle: String? = null,
    val company: String? = null,
    val aboutMe: String? = null,
    val profileImageUrl: String? = null
)

data class AddConnectionRequest(
    val email: String
)

data class SearchUsersResponse(
    val users: List<UserSearchResult>
)

data class ConnectionsResponse(
    val connections: List<ConnectionResponse>
)

data class ConnectionResponse(
    val userId: Long,
    val name: String,
    val company: String?,
    val profileImageUrl: String?
)

data class ProfileStatusResponse(
    val profileCompleted: Boolean
)