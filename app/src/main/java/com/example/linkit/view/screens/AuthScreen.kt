package com.example.linkit.view.screens

import com.example.linkit.R
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.linkit.ui.theme.bcg2
import com.example.linkit.ui.theme.bcg3
import com.example.linkit.util.UiEvent
import com.example.linkit.view.components.LoadingIndicator
import com.example.linkit.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest


@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
                    scaffoldState.snackbarHostState.showSnackbar(event.msg)
                }
                else -> Unit
            }
        }
    }

    Scaffold(scaffoldState = scaffoldState) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
//                .background(bcg3)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 25.dp)
                    .padding(bottom = 180.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Login Card
                    AuthOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "Login",
                        subtitle = "Already have an account?",
                        imageRes = R.drawable.login_img,
                        isSelected = state.isLoginMode,
                        onClick = {
                            if (!state.isLoginMode) viewModel.toggleMode()
                        }
                    )

                    // Sign Up Card
                    AuthOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "Sign Up",
                        subtitle = "Don't have an account?",
                        imageRes = R.drawable.signup_img,
                        isSelected = !state.isLoginMode,
                        onClick = {
                            if (state.isLoginMode) viewModel.toggleMode()
                        }
                    )
                }


                Spacer(modifier = Modifier.height(20.dp))
                Crossfade(targetState = state.isLoginMode, label = "auth_fields") { isLogin ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!isLogin) {
                            OutlinedTextField(
                                value = state.username,
                                onValueChange = { viewModel.onUsernameChanged(it) },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = { viewModel.onEmailChange(it) },
                                label = { Text("Email") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = state.password,
                                onValueChange = { viewModel.onPasswordChanged(it) },
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = state.username,
                                onValueChange = { viewModel.onUsernameChanged(it) },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = state.password,
                                onValueChange = { viewModel.onPasswordChanged(it) },
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = viewModel::onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(12.dp),

                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bcg2, // Set the background color
                        contentColor = Color.Black, // Set the text color
                        disabledContainerColor = bcg2 // Set background when disabled
                )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isLoading) {
                            LoadingIndicator()
                        } else {
                            Text(
                                text = if (state.isLoginMode) "Login" else "Sign Up",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun AuthOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    @DrawableRes imageRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dropShadowStyle = TextStyle(
        color = Color.Black,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp
    )

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .shadow(12.dp, RoundedCornerShape(12.dp)),
        elevation = if (isSelected) 12.dp else 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        border = if (isSelected) BorderStroke(3.dp, bcg2) else null
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .height(150.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = dropShadowStyle,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = Color.Gray,
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center
            )
        }
    }
}

