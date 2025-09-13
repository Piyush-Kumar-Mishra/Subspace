package com.example.linkit.view.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.linkit.R
import com.example.linkit.data.models.ConnectionResponse
import com.example.linkit.data.models.UserSearchResult
import com.example.linkit.util.Constants
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.UiEvent
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
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
        viewModel.refreshAllData()
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

// Listen to offline/online state changes
    LaunchedEffect(uiState.isOffline) {
        if (uiState.isOffline) {
            Toast.makeText(
                context,
                uiState.offlineMessage.ifBlank { "You are offline" },
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Back online",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    Scaffold(

        snackbarHost = { SnackbarHost(snackbarHostState) },

        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAllData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        val currentProfileState = profileState
        when (currentProfileState) {
            is NetworkResult.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is NetworkResult.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    item {
                        ProfileHeader(
                            profile = currentProfileState.data,
                            profileImageBitmap = state.profileImageBitmap,
                            onImageClick = { imagePickerLauncher.launch("image/*") },
                            onAddConnectionClick = { showBottomSheet = true }
                        )
                    }
                    item {
                        EditableProfileFields(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    item {
                        ProfileTabs(
                            selectedTabIndex = state.selectedTab,
                            onTabSelected = viewModel::onTabSelected
                        )
                    }
                    when (state.selectedTab) {
                        0 -> { // Connections
                            if (state.connections.isEmpty()) {
                                item { NoConnectionsView() }
                            } else {
                                items(state.connections) { connection ->
                                    ConnectionItem(connection = connection)
                                }
                            }
                        }
                        1 -> { // About
                            item {
                                AboutSection(
                                    aboutText = state.aboutMe,
                                    onAboutChanged = viewModel::onAboutMeChanged
                                )
                            }
                        }
                    }
                }
            }
            is NetworkResult.Error -> {
                ErrorView(
                    message = currentProfileState.message,
                    onRetry = onNavigateBack
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = bottomSheetState
            ) {
                AddConnectionBottomSheet(
                    searchQuery = state.searchQuery,
                    searchResults = state.searchResults,
                    onSearchQueryChanged = viewModel::searchUsers,
                    onAddConnection = { email ->
                        viewModel.addConnection(email)
                        coroutineScope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: com.example.linkit.data.models.ProfileResponse,
    profileImageBitmap: Bitmap?,
    onImageClick: () -> Unit,
    onAddConnectionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Welcome, ${profile.name}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onImageClick() }
        ) {
            when {
                // Show selected bitmap first (when updating)
                profileImageBitmap != null -> {
                    Image(
                        bitmap = profileImageBitmap.asImageBitmap(),
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Show existing profile image from server
                !profile.profileImageUrl.isNullOrBlank() -> {
                    AuthorizedAsyncImage(
                        imageUrl = profile.profileImageUrl,
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Show placeholder
                else -> {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Add Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (!profile.jobTitle.isNullOrBlank()) {
            Text(
                text = profile.jobTitle,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (!profile.company.isNullOrBlank()) {
            Text(
                text = profile.company,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddConnectionClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Connection")
        }
    }
}
@Composable
private fun AuthorizedAsyncImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val fullImageUrl = remember(imageUrl) {
        if (imageUrl.startsWith("http")) imageUrl
        else "${Constants.BASE_URL}${imageUrl.removePrefix("/")}"
    }

    AsyncImage(
        model = fullImageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        placeholder = painterResource(R.drawable.ic_person),
        error = painterResource(R.drawable.ic_person)
    )
}


@Composable
private fun EditableProfileFields(state: ProfileUiState, viewModel: ProfileViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChanged,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.jobTitle,
            onValueChange = viewModel::onJobTitleChanged,
            label = { Text("Job Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.company,
            onValueChange = viewModel::onCompanyChanged,
            label = { Text("Company") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = viewModel::updateProfile,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Update Profile")
            }
        }
    }
}

@Composable
private fun ProfileTabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    TabRow(selectedTabIndex = selectedTabIndex) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            text = { Text("Connections") }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            text = { Text("About") }
        )
    }
}

@Composable
private fun ConnectionItem(connection: ConnectionResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!connection.profileImageUrl.isNullOrBlank()) {
                    AuthorizedAsyncImage(
                        imageUrl = connection.profileImageUrl,
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = connection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!connection.company.isNullOrBlank()) {
                    Text(
                        text = connection.company,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSection(aboutText: String, onAboutChanged: (String) -> Unit) {
    OutlinedTextField(
        value = aboutText,
        onValueChange = onAboutChanged,
        label = { Text("About Me") },
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(16.dp),
        maxLines = 6
    )
}

@Composable
private fun AddConnectionBottomSheet(
    searchQuery: String,
    searchResults: List<UserSearchResult>,
    onSearchQueryChanged: (String) -> Unit,
    onAddConnection: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Add Connection",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text("Search by email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
            Text(text = "No users found", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn {
                items(searchResults) { user ->
                    SearchUserItem(
                        user = user,
                        onAddClick = { onAddConnection(user.email) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchUserItem(user: UserSearchResult, onAddClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!user.profileImageUrl.isNullOrBlank()) {
                    AuthorizedAsyncImage(
                        imageUrl = user.profileImageUrl,
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = user.email, style = MaterialTheme.typography.bodyMedium)
                if (!user.company.isNullOrBlank()) {
                    Text(
                        text = user.company,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(onClick = onAddClick) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun NoConnectionsView() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No connections yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error: $message", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Go Back")
            }
        }
    }
}


