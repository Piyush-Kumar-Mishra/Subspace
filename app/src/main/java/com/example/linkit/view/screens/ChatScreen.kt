package com.example.linkit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.linkit.data.models.ChatMessageResponse
import com.example.linkit.view.components.AuthorizedAsyncImage
import com.example.linkit.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

val ChatOwnMessageBackground = Color(0xFF007AFF)
val ChatOtherMessageBackground = Color(0xFFE9E9EB)
val ChatSystemMessageBackground = Color(0xFFF0F0F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    projectId: Long,
    projectName: String? = null
) {
    val viewModel: ChatViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(projectId) {
        viewModel.initializeChat(projectId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.disconnect() }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                projectName = projectName ?: "Project Chat",
                onlineCount = uiState.onlineUsers.size,
                isConnected = uiState.isConnected,
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Column {
                if (uiState.typingUsers.isNotEmpty()) {
                    TypingIndicator(typingUsers = uiState.typingUsers)
                }
                MessageInput(
                    messageText = uiState.inputText,
                    isSending = uiState.isSending,
                    onTextChange = viewModel::onInputTextChanged,
                    onSendClick = viewModel::sendMessage
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                MessageList(
                    messages = uiState.messages,
                    listState = listState,
                    onLoadMore = viewModel::loadMoreMessages,
                    isLoadingMore = uiState.isLoadingMore,
                    hasMore = uiState.hasMoreMessages
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    projectName: String,
    onlineCount: Int,
    isConnected: Boolean,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color.Green else Color.Red)
                    )
                    Text(
                        text = "$onlineCount online",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
fun MessageList(
    messages: List<ChatMessageResponse>,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    hasMore: Boolean
) {
    val scope = rememberCoroutineScope()

    val newestMessageId = messages.firstOrNull()?.id
    val isUserScrolling = listState.isScrollInProgress

    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }

    //Auto-Scroll Logic
    LaunchedEffect(newestMessageId) {
        if (messages.isNotEmpty()) {
            val isOwnMessage = messages.first().isOwnMessage
            // Auto-scroll if:
            // - I sent the message
            // - OR I am already looking at the bottom
            // - OR it's the very first load
            if (isOwnMessage || !showScrollToBottom) {
                listState.animateScrollToItem(0)
            }
        }
    }

    // Pagination (History) Logic
    val totalItems = listState.layoutInfo.totalItemsCount
    val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    LaunchedEffect(lastVisibleIndex, totalItems) {
        if (totalItems > 0 &&
            lastVisibleIndex >= totalItems - 5 && // Load when 5 items away from top
            hasMore &&
            !isLoadingMore
        ) {
            onLoadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true, // Bottom is Index 0
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(
                items = messages,
                key = { it.id }
            ) { message ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Date Header (Visually Top)
                    if (message.showDateHeader) {
                        DateHeader(message.dateHeader ?: "")
                    }

                    when (message.messageType) {
                        "USER_MESSAGE" -> UserMessageItem(
                            message = message,
                            isOwnMessage = message.isOwnMessage
                        )
                        "SYSTEM_MESSAGE" -> SystemMessageItem(message = message)
                        else -> SystemMessageItem(message = message)
                    }
                }
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Floating "New Messages" Button
        AnimatedVisibility(
            visible = showScrollToBottom,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll Down")
            }
        }
    }
}

@Composable
fun DateHeader(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ChatSystemMessageBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun UserMessageItem(message: ChatMessageResponse, isOwnMessage: Boolean) {
    Row(
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isOwnMessage) {
            // Profile Icon Logic
            if (!message.sender?.profileImageUrl.isNullOrBlank()) {
                AuthorizedAsyncImage(
                    imageUrl = message.sender.profileImageUrl,
                    contentDescription = message.sender.name ?: "User",
                    modifier = Modifier.size(32.dp).clip(CircleShape).align(Alignment.Bottom)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray).padding(4.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start) {
            if (!isOwnMessage && message.sender != null) {
                Text(
                    text = message.sender.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isOwnMessage) 18.dp else 4.dp,
                    bottomEnd = if (isOwnMessage) 4.dp else 18.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOwnMessage) ChatOwnMessageBackground else ChatOtherMessageBackground
                )
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isOwnMessage) Color.White else Color.Black
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Text(
                text = message.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun SystemMessageItem(message: ChatMessageResponse) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun MessageInput(
    messageText: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            shape = RoundedCornerShape(24.dp),
            maxLines = 4
        )
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(
            onClick = onSendClick,
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier.size(50.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun TypingIndicator(typingUsers: Map<Long, String>) {
    val typingText = when {
        typingUsers.isEmpty() -> ""
        typingUsers.size == 1 -> "${typingUsers.values.first()} is typing..."
        typingUsers.size == 2 -> "${typingUsers.values.first()} and ${typingUsers.values.last()} are typing..."
        else -> "${typingUsers.size} people are typing..."
    }

    if (typingText.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = typingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
