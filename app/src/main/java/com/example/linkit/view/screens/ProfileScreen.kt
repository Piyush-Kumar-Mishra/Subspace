package com.example.linkit.view.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.linkit.data.models.ConnectionResponse
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.UiEvent
import com.example.linkit.view.components.AuthorizedAsyncImage
import com.example.linkit.view.components.LoadingIndicator
import com.example.linkit.viewmodel.ProfileUiState
import com.example.linkit.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val profileState by viewModel.profileState.collectAsState()
    val viewedProfileState by viewModel.viewedProfileState.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val viewedProfileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showUpdateProfileSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            viewModel.onImageSelected(bitmap)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> snackbarHostState.showSnackbar(event.msg)
                UiEvent.NavigateToAuth -> onNavigateBack()
                else -> Unit
            }
        }
    }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val currentProfile = profileState) {
                is NetworkResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp, color = Color.Black)
                    }
                }
                is NetworkResult.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            ProfileHeader(
                                profile = currentProfile.data,
                                profileImageBitmap = state.profileImageBitmap,
                                onLogoutClick = { showLogoutDialog = true }
                            )
                        }

                        item {
                            ActionsRow(
                                onEditClick = { showUpdateProfileSheet = true },
                                connectionsCount = state.connections.size
                            )
                        }

                        item {
                            AboutSection(aboutText = state.aboutMe)
                        }

                        if (state.connections.isNotEmpty()) {
                            item {
                                ConnectionList(
                                    connections = state.connections,
                                    onConnectionClick = { id ->
                                        if (id != currentProfile.data.userId) viewModel.viewUserProfile(id)
                                    }
                                )
                            }
                        }

                        item {
                            DeleteAccountSection(onDeleteClick = { showDeleteDialog = true })
                        }

                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }
                is NetworkResult.Error -> {
                    ErrorView(message = currentProfile.message, onRetry = onNavigateBack)
                }
            }
        }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete your profile, projects, and all associated data. This action is irreversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete Forever", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.Black)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to end your current session?") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout() }) {
                    Text("Confirm", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.Black)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (viewedProfileState.profile != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeUserProfileSheet() },
            sheetState = viewedProfileSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            UserProfileSheet(
                state = viewedProfileState,
                onClose = {
                    coroutineScope.launch { viewedProfileSheetState.hide() }
                        .invokeOnCompletion { if (!viewedProfileSheetState.isVisible) viewModel.closeUserProfileSheet() }
                }
            )
        }
    }

    if (showUpdateProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUpdateProfileSheet = false },
            sheetState = editSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray.copy(0.5f)) }
        ) {
            UpdateProfileSheet(
                state = state,
                viewModel = viewModel,
                onDismiss = {
                    coroutineScope.launch { editSheetState.hide() }
                        .invokeOnCompletion { showUpdateProfileSheet = false }
                },
                onImageClick = { imagePickerLauncher.launch("image/*") },
                existingImageUrl = (profileState as? NetworkResult.Success)?.data?.profileImageUrl
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: com.example.linkit.data.models.ProfileResponse,
    profileImageBitmap: Bitmap?,
    onLogoutClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Blue.copy(0.8f), Color.Transparent)
                    )
                )
        )

        IconButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 12.dp,
                    border = BorderStroke(4.dp, Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                        if (profileImageBitmap != null) {
                            Image(
                                bitmap = profileImageBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!profile.profileImageUrl.isNullOrBlank()) {
                            AuthorizedAsyncImage(
                                imageUrl = profile.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                cacheKey = profile.userId.toString()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F3F5)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp), tint = Color(0xFFCED4DA))
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.size(36.dp).offset(x = (-4).dp, y = (-4).dp),
                    shape = CircleShape,
                    color = Color.Black,
                    shadowElevation = 4.dp
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.padding(8.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Surface(
                color = Color.Black.copy(0.05f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = if (!profile.jobTitle.isNullOrBlank()) "${profile.jobTitle} @ ${profile.company}" else "Professional User",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActionsRow(onEditClick: () -> Unit, connectionsCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onEditClick,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit Profile", fontWeight = FontWeight.Bold)
        }

        OutlinedCard(
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(0.4f)),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(connectionsCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connections", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun AboutSection(aboutText: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            "Bio",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = aboutText.ifBlank { "Tell about yourself..." },
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
            color = if (aboutText.isBlank()) Color.LightGray else Color(0xFF495057),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun ConnectionList(
    connections: List<ConnectionResponse>,
    onConnectionClick: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            "My Network",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp),
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(connections) { connection ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(70.dp).clickable { onConnectionClick(connection.userId) }
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        if (!connection.profileImageUrl.isNullOrBlank()) {
                            AuthorizedAsyncImage(
                                imageUrl = connection.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                cacheKey = connection.userId.toString()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = Color.LightGray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connection.name.split(" ").first(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountSection(onDeleteClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 64.dp), color = Color.LightGray.copy(0.3f))

        TextButton(
            onClick = onDeleteClick,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Delete Account Permanently", fontWeight = FontWeight.Bold)
        }
        Text(
            "This action is permanent and clears all data from our servers.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun UpdateProfileSheet(
    state: ProfileUiState,
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit,
    onImageClick: () -> Unit,
    existingImageUrl: String?
) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Update Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.size(90.dp).clickable { onImageClick() }) {
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFF5F5F7))) {
                when {
                    state.profileImageBitmap != null -> Image(bitmap = state.profileImageBitmap.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    !existingImageUrl.isNullOrBlank() -> AuthorizedAsyncImage(existingImageUrl, null, modifier = Modifier.fillMaxSize())
                    else -> Icon(Icons.Default.CameraAlt, null, modifier = Modifier.align(Alignment.Center), tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val textFieldColors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Black, unfocusedIndicatorColor = Color.LightGray)

        TextField(value = state.name, onValueChange = viewModel::onNameChanged, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        TextField(value = state.jobTitle, onValueChange = viewModel::onJobTitleChanged, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        TextField(value = state.company, onValueChange = viewModel::onCompanyChanged, label = { Text("Company") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        TextField(value = state.aboutMe, onValueChange = viewModel::onAboutMeChanged, label = { Text("About Me") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, minLines = 3)

        Spacer(modifier = Modifier.height(26.dp))

        Button(onClick = { viewModel.updateProfile(); onDismiss() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
            if (state.isLoading) LoadingIndicator() else Text("Save Changes", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(80.dp), color = Color(0xFFFFEBEE), shape = CircleShape) {
                Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.padding(20.dp), tint = Color(0xFFD32F2F))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Oops! Connection Lost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = message,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
            ) { Text("Try Again") }
        }
    }
}