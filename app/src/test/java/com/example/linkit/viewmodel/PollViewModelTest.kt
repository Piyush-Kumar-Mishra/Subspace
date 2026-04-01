package com.example.linkit.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.linkit.data.models.CreatePollRequest
import com.example.linkit.data.models.PollOptionResponse
import com.example.linkit.data.models.PollResponse
import com.example.linkit.data.models.ProjectAssigneeResponse
import com.example.linkit.data.repo.PollRepository
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.UiEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class PollViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    @MockK
    lateinit var pollRepository: PollRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `removePollOption keeps two options minimum and removes extras when available`() = runTest {
        val viewModel = createViewModel()
        viewModel.onPollOptionChanged(0, "A")
        viewModel.onPollOptionChanged(1, "B")
        viewModel.removePollOption(0)

        assertEquals(listOf("A", "B"), viewModel.pollState.value.options)

        viewModel.addPollOption()
        viewModel.onPollOptionChanged(2, "Demo day")
        viewModel.removePollOption(1)

        assertEquals(listOf("A", "Demo day"), viewModel.pollState.value.options)
    }

    @Test
    fun `createPoll with invalid form shows validation message and skips repository call`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPollOptionChanged(0, "Option A")
        viewModel.onPollOptionChanged(1, "")

        viewModel.uiEvent.test {
            viewModel.createPoll(PROJECT_ID)
            advanceUntilIdle()

            assertEquals(
                UiEvent.ShowToast("Question and at least two options are required."),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 0) { pollRepository.createProjectPoll(any(), any()) }
    }

    @Test
    fun `createPoll success filters blank options emits success events and clears form`() = runTest {
        val requestSlot = slot<CreatePollRequest>()
        every {
            pollRepository.createProjectPoll(PROJECT_ID, capture(requestSlot))
        } returns flowOf(
            NetworkResult.Loading(),
            NetworkResult.Success(samplePoll())
        )
        val viewModel = createViewModel()

        viewModel.onPollQuestionChanged("Which day works best?")
        viewModel.onPollOptionChanged(0, "Monday")
        viewModel.onPollOptionChanged(1, "")
        viewModel.addPollOption()
        viewModel.onPollOptionChanged(2, "Tuesday")
        viewModel.onAllowMultipleAnswersChanged(true)

        viewModel.uiEvent.test {
            viewModel.createPoll(PROJECT_ID)
            advanceUntilIdle()

            assertEquals(UiEvent.ShowToast("Poll created!"), awaitItem())
            assertEquals(UiEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("Which day works best?", requestSlot.captured.question)
        assertEquals(listOf("Monday", "Tuesday"), requestSlot.captured.options)
        assertEquals(true, requestSlot.captured.allowMultipleAnswers)
        assertEquals(PollUiState(), viewModel.pollState.value)
    }

    @Test
    fun `getPoll with not found clears loading without showing a toast`() = runTest {
        every { pollRepository.getProjectPoll(PROJECT_ID) } returns flowOf(
            NetworkResult.Loading(),
            NetworkResult.Error("NOT_FOUND")
        )
        val viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.getPoll(PROJECT_ID)
            advanceUntilIdle()

            assertFalse(viewModel.pollState.value.isLoading)
            assertNull(viewModel.pollState.value.poll)
            expectNoEvents()
        }
    }

    @Test
    fun `voteOnPoll error shows toast`() = runTest {
        every { pollRepository.voteOnPoll(POLL_ID, OPTION_ID) } returns flowOf(
            NetworkResult.Error("Unable to submit vote")
        )
        val viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.voteOnPoll(POLL_ID, OPTION_ID)
            advanceUntilIdle()

            assertEquals(UiEvent.ShowToast("Unable to submit vote"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(): PollViewModel = PollViewModel(pollRepository)

    private fun samplePoll() = PollResponse(
        id = POLL_ID,
        projectId = PROJECT_ID,
        question = "Which day works best?",
        allowMultipleAnswers = true,
        createdBy = ProjectAssigneeResponse(
            userId = 5L,
            name = "kelson",
            profileImageUrl = null
        ),
        options = listOf(
            PollOptionResponse(
                id = OPTION_ID,
                optionText = "Monday",
                votes = emptyList(),
                voteCount = 0
            ),
            PollOptionResponse(
                id = OPTION_ID + 1,
                optionText = "Tuesday",
                votes = emptyList(),
                voteCount = 0
            )
        ),
        totalVotes = 0
    )

    companion object {
        private const val PROJECT_ID = 11L
        private const val POLL_ID = 21L
        private const val OPTION_ID = 31L
    }
}
