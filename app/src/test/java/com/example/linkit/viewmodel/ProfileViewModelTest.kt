package com.example.linkit.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.linkit.data.TokenStore
import com.example.linkit.data.models.ConnectionsResponse
import com.example.linkit.data.models.ProfileResponse
import com.example.linkit.data.models.SearchUsersResponse
import com.example.linkit.data.models.UserSearchResult
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ProfileRepository
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.NetworkUtils
import com.example.linkit.util.UiEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    @MockK
    lateinit var profileRepository: ProfileRepository
    @MockK
    lateinit var authRepository: AuthRepository
    @MockK
    lateinit var networkUtils: NetworkUtils
    @MockK(relaxed = true)
    lateinit var tokenStore: TokenStore

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(dispatcher)

        every { networkUtils.networkStatus } returns flowOf(true)
        every { authRepository.getToken() } returns flowOf("token")
        every { profileRepository.getUserProfile() } returns flowOf(NetworkResult.Loading())
        every { profileRepository.getConnections() } returns flowOf(
            NetworkResult.Success(ConnectionsResponse(emptyList()))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads profile details into ui state`() = runTest {
        val profile = ProfileResponse(
            userId = 7L,
            name = "Aman Verma",
            jobTitle = "Android Developer",
            company = "LinkIt",
            aboutMe = "Building useful things"
        )
        val viewModel = createViewModel(
            profileFlow = flowOf(
                NetworkResult.Loading(),
                NetworkResult.Success(profile)
            )
        )

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(7L, uiState.userId)
        assertEquals("Aman Verma", uiState.name)
        assertEquals("Android Developer", uiState.jobTitle)
        assertEquals("LinkIt", uiState.company)
        assertEquals("Building useful things", uiState.aboutMe)
        assertTrue(viewModel.profileState.value is NetworkResult.Success)
    }

    @Test
    fun `createProfile with blank name shows validation message and skips repository call`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEvent.test {
            viewModel.createProfile()
            advanceUntilIdle()

            assertEquals(UiEvent.ShowToast("Name is required"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 0) { profileRepository.createProfile(any()) }
    }

    @Test
    fun `onNameChanged keeps previous value when input is longer than limit`() = runTest {
        val viewModel = createViewModel()

        viewModel.onNameChanged("Aman")
        viewModel.onNameChanged("This name is definitely longer than twenty four")

        assertEquals("Aman", viewModel.uiState.value.name)
    }

    @Test
    fun `searchUsers with blank query clears previous search results`() = runTest {
        val users = listOf(
            UserSearchResult(
                userId = 5L,
                name = "Riya Sharma",
                email = "riya@example.com",
                company = "LinkIt",
                profileImageUrl = null
            )
        )
        every { profileRepository.searchUsers("riya") } returns flowOf(
            NetworkResult.Loading(),
            NetworkResult.Success(SearchUsersResponse(users))
        )

        val viewModel = createViewModel()

        viewModel.searchUsers("riya")
        advanceUntilIdle()
        assertEquals(users, viewModel.uiState.value.searchResults)

        viewModel.searchUsers("")

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
        verify(exactly = 1) { profileRepository.searchUsers("riya") }
    }

    private fun createViewModel(
        profileFlow: Flow<NetworkResult<ProfileResponse>> = flowOf(NetworkResult.Loading()),
        connectionsFlow: Flow<NetworkResult<ConnectionsResponse>> = flowOf(
            NetworkResult.Success(ConnectionsResponse(emptyList()))
        )
    ): ProfileViewModel {
        every { profileRepository.getUserProfile() } returns profileFlow
        every { profileRepository.getConnections() } returns connectionsFlow

        return ProfileViewModel(
            profileRepository,
            authRepository,
            networkUtils,
            tokenStore
        )
    }
}
