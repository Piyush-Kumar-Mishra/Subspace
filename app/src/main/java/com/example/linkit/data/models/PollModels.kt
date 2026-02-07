package com.example.linkit.data.models

data class CreatePollRequest(
    val question: String,
    val options: List<String>,
    val allowMultipleAnswers: Boolean
)

data class PollVoteResponse(
    val user: ProjectAssigneeResponse
)

data class PollOptionResponse(
    val id: Long,
    val optionText: String,
    val votes: List<PollVoteResponse>,
    val voteCount: Int
)

data class PollResponse(
    val id: Long,
    val projectId: Long,
    val question: String,
    val allowMultipleAnswers: Boolean,
    val createdBy: ProjectAssigneeResponse,
    val options: List<PollOptionResponse>,
    val totalVotes: Int
)
