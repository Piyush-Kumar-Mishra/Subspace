package com.example.linkit.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.linkit.data.repo.AuthRepository
import com.example.linkit.data.repo.ProfileRepository
import com.example.linkit.util.NetworkUtils
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    @MockK
    lateinit var authRepository: AuthRepository

    @MockK
    lateinit var profileRepository: ProfileRepository

    @MockK
    lateinit var networkUtils: NetworkUtils

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(dispatcher)
        every { networkUtils.networkStatus } returns flowOf(true)

        viewModel = AuthViewModel(
            authRepository,
            profileRepository,
            networkUtils
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.value

        Assert.assertTrue(state.isLoginMode)
        Assert.assertFalse(state.isLoading)
        Assert.assertEquals("", state.email)
    }

    @Test
    fun `toggleMode switches login mode`() = runTest {
        Assert.assertTrue(viewModel.uiState.value.isLoginMode)

        viewModel.toggleMode()

        Assert.assertFalse(viewModel.uiState.value.isLoginMode)
    }

    @Test
    fun `onEmailChange updates email`() = runTest {
        viewModel.onEmailChange("test@example.com")

        Assert.assertEquals(
            "test@example.com",
            viewModel.uiState.value.email
        )
    }

    @Test
    fun `onSubmit with empty fields shows error`() = runTest {
        viewModel.onUsernameChanged("")
        viewModel.onPasswordChanged("")

        viewModel.onSubmit()

        Assert.assertEquals(
            "Required",
            viewModel.uiState.value.usernameError
        )
    }
}