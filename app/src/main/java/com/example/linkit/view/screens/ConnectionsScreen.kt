package com.example.linkit.view.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.linkit.data.models.ConnectionResponse
import com.example.linkit.data.models.UserConnection
import com.example.linkit.data.models.UserSearchResult
import com.example.linkit.view.components.AuthorizedAsyncImage
import com.example.linkit.viewmodel.ProfileViewModel

@Composable
fun ConnectionsScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showBlockDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<ConnectionResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPendingRequests()
        viewModel.refreshAllData()
    }


        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 2.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            item {
                Text(
                    text = "Connections",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.Black
                )
            }

            item {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.searchUsers(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(30.dp), spotColor = Color.Black),
                    placeholder = {
                        Text(
                            "Search with email....",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.DarkGray
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchUsers("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            if (uiState.searchResults.isNotEmpty()) {
                item {
                    SectionHeader(title = "Results")
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        items(uiState.searchResults) { user ->
                            SearchResultItem(
                                user = user,
                                onConnect = { viewModel.addConnection(user.email) }
                            )
                        }
                    }
                }
                item { HorizontalDivider(color = Color(0xFFEEEEEE)) }
            }

            if (uiState.pendingRequests.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Invitations",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(Modifier.width(8.dp))
                        Badge(
                            containerColor = Color.Green,
                            contentColor = Color.White
                        ) {
                            Text(uiState.pendingRequests.size.toString(), color = Color.White)
                        }
                    }
                }

                items(uiState.pendingRequests) { request ->
                    ConnectionRequestItem(
                        request = request,
                        onAccept = { viewModel.acceptRequest(request.id) },
                        onReject = { viewModel.rejectRequest(request.id) }
                    )

                }

                item { HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 2.dp) }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,

                ) {
                    Text(
                        "My Network",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(0.8f)
                    )
                    Text(
                        "${uiState.connections.size} connections",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha= 0.8f)
                    )
                }
            }

            if (uiState.connections.isEmpty()) {
                item {
                    EmptyStateView()
                }
            } else {
                items(uiState.connections) { connection ->
                    ConnectionItem(
                        connection = connection,
                        onRemove = { viewModel.removeConnection(connection.userId) },
                        onBlock = {
                            userToBlock = connection
                            showBlockDialog = true
                        }
                    )
                }
            }
        }

    if (showBlockDialog) {
        userToBlock?.let { user ->
            BlockUserDialog(
                userName = user.name,
                onConfirmBlock = {
                    viewModel.blockUser(user.userId)
                    showBlockDialog = false
                    userToBlock = null
                },
                onDismiss = {
                    showBlockDialog = false
                    userToBlock = null
                }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.DarkGray
    )
}
@Composable
fun SearchResultItem(
    user: UserSearchResult,
    onConnect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            AuthorizedAsyncImage(
                imageUrl = user.profileImageUrl,
                contentDescription = null,
                isOffline = false,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Black.copy(alpha = 0.8f), CircleShape)
                    .background(Color(0xFFF0F0F0))
            )

            Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = user.company ?: "No Company",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(31.dp),
                shape = RoundedCornerShape(44),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.8f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

    }
}

@Composable
fun ConnectionRequestItem(
    request: UserConnection,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Green.copy(alpha = 0.8f), CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            if (!request.requesterImage.isNullOrBlank()) {
                AsyncImage(
                    model = request.requesterImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = Color.LightGray)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.requesterName ?: "User ${request.requesterId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            if (!request.requesterCompany.isNullOrBlank()) {
                Text(
                    text = request.requesterCompany,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier
                    .height(34.dp)
                    .defaultMinSize(minWidth = 1.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Text("Ignore", fontSize = 12.sp)
            }

            Button(
                onClick = onAccept,
                modifier = Modifier
                    .height(34.dp)
                    .defaultMinSize(minWidth = 1.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.8f))
            ) {
                Text("Accept", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ConnectionItem(
    connection: ConnectionResponse,
    onRemove: () -> Unit,
    onBlock: (ConnectionResponse) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuthorizedAsyncImage(
            imageUrl = connection.profileImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = connection.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            val job = connection.company
            val company = connection.company

            if (!job.isNullOrBlank() || !company.isNullOrBlank()) {
                val subtitle = when {
                    job.isNotBlank() && company.isNotBlank() -> "$job • $company"
                    job.isNotBlank() -> job
                    else -> company
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onBlock(connection) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(2.dp, Color.Red),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Block", fontSize = 12.sp)
            }

            Button(
                onClick = onRemove,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(0.8f),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Unfollow", fontSize = 12.sp)
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
}


@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = Color.LightGray
        )
        Text(
            "No connections yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            "Search for people to build your network",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )
    }
}

@Composable
fun BlockUserDialog(
    userName: String,
    onConfirmBlock: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block User") },
        text = { Text("Are you sure you want to block $userName? ")},
        confirmButton = {
            Button(
                onClick = onConfirmBlock,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
