package com.example.linkit.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CreatePollRequest(
    val question: String,
    val options: List<String>,
    val allowMultipleAnswers: Boolean
)

@Serializable
data class PollVoteResponse(
    val user: ProjectAssigneeResponse
)

@Serializable
data class PollOptionResponse(
    val id: Long,
    val optionText: String,
    val votes: List<PollVoteResponse>,
    val voteCount: Int
)

@Serializable
data class PollResponse(
    val id: Long,
    val projectId: Long,
    val question: String,
    val allowMultipleAnswers: Boolean,
    val createdBy: ProjectAssigneeResponse,
    val options: List<PollOptionResponse>,
    val totalVotes: Int
)
