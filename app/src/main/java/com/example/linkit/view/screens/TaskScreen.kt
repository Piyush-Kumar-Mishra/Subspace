package com.example.linkit.view.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.linkit.R
import com.example.linkit.data.models.*
import com.example.linkit.ui.theme.AntonFontFamily
import com.example.linkit.ui.theme.grenze_blackItalic
import com.example.linkit.ui.theme.matalmania
import com.example.linkit.ui.theme.odisansFamily
import com.example.linkit.util.UiEvent
import com.example.linkit.view.components.AuthorizedAsyncImage
import com.example.linkit.view.components.BrushStrokeShape
import com.example.linkit.viewmodel.PollViewModel
import com.example.linkit.viewmodel.ProfileViewModel
import com.example.linkit.viewmodel.ProjectViewModel
import com.example.linkit.viewmodel.ViewedProfileState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    projectId: Long,
    viewModel: ProjectViewModel,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCreateTask: (Long) -> Unit,
    onNavigateToCreatePoll: (Long) -> Unit,
    onNavigateToChat: (Long) -> Unit,
    onNavigateToAnalytics: (Long) -> Unit

) {
    val uiState by viewModel.uiState.collectAsState()
    val createProjectState by viewModel.createProjectState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState()}
    val viewedProfileState by profileViewModel.viewedProfileState.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showBottomSheet = remember { derivedStateOf { viewedProfileState.profile != null } }
    val pollViewModel: PollViewModel = hiltViewModel()
    val pollState by pollViewModel.pollState.collectAsState()
    var showPollDialog by remember { mutableStateOf(false) }
    var selectedTaskForDetail by remember { mutableStateOf<TaskResponse?>(null) }

    if (showPollDialog && pollState.poll != null) {
        PollDialog(
            poll = pollState.poll!!,
            loggedInUserId = uiState.loggedInUserId,
            onDismissRequest = { showPollDialog = false },
            onVote = { pollId, optionId ->
                pollViewModel.voteOnPoll(pollId, optionId)
            }
        )
    }

    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { profileViewModel.closeUserProfileSheet() },
            sheetState = sheetState,
            scrimColor = Color.Black.copy(alpha = 0.5f),
        ) {
            UserProfileSheet(
                state = viewedProfileState,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            profileViewModel.closeUserProfileSheet()
                        }
                    }
                }
            )
        }
    }
    if (selectedTaskForDetail != null) {
        TaskDetailBottomSheet(
            task = selectedTaskForDetail!!,
            onDismiss = { selectedTaskForDetail = null }
        )
    }


    LaunchedEffect(projectId) {
        viewModel.loadProjectById(projectId)
        viewModel.loadProjectTasks(projectId)
        viewModel.uiEvent.collectLatest { event ->
             when (event) {
                is UiEvent.ShowToast -> snackbarHostState.showSnackbar(event.msg)
                UiEvent.NavigateBack -> onNavigateBack()
                else -> Unit
            }
        }
    }


    if (uiState.showDeleteProjectDialog) {
        DeleteProjectConfirmationDialog(
            projectName = uiState.currentProject?.name ?: "this project",
            onConfirm = { viewModel.confirmDeleteCurrentProject() },
            onDismiss = { viewModel.onDismissDeleteProjectDialog() }
        )
    }

    if (createProjectState.showAssigneeDialog) {
        AssigneeSelectionDialog(
            assignees = createProjectState.availableAssignees,
            selectedAssignees = createProjectState.selectedAssignees,
            onAssigneeSelected = { viewModel.addAssignee(it) },
            onAssigneeDeselected = { viewModel.removeAssignee(it) },
            onSave = { viewModel.updateProject() },
            onDismiss = {
                viewModel.toggleAssigneeDialog()
                viewModel.clearProjectForm()
            }
        )
    }




    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFEDF1F3)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            var isNavigating by remember { mutableStateOf(false) }
            ProjectHeader(
                project = uiState.currentProject,
                taskCount = uiState.tasks.size,
                loggedInUserId = uiState.loggedInUserId,
                projectHasPoll = uiState.projectHasPoll,

                onNavigateBack = {
                    if (!isNavigating) {
                        isNavigating = true
                        viewModel.goBackToProjects()
                        onNavigateBack()
                    }
                },
                onDeleteProject = { viewModel.onDeleteProjectClicked() },
                onAddAssignees = {
                    viewModel.loadProjectForEditing(projectId, openAssigneeDialog = true)
                },

                onNavigateToCreatePoll = { onNavigateToCreatePoll(projectId) },
                onViewPollClicked = {
                    pollViewModel.getPoll(projectId)
                    showPollDialog = true
                },
                        onNavigateToChat = { onNavigateToChat(projectId) },
                onNavigateToAnalytics = { onNavigateToAnalytics(projectId) }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.tasks.isEmpty()) {
                EmptyTasksView(
                    modifier = Modifier.weight(1f),
                    onCreateTask = { onNavigateToCreateTask(projectId) }
                )
            } else {
                TaskCard(
                    tasks = uiState.tasks,
                    attachmentsByTask = uiState.attachmentsByTask,
                    loggedInUserId = uiState.loggedInUserId,
                    onAssigneeClick = { userId ->
                        profileViewModel.viewUserProfile(userId)
                    },
                    viewModel = viewModel,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onTaskClick = { task -> selectedTaskForDetail = task }

                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(10.dp).navigationBarsPadding()) {
            if (uiState.currentProject != null) {
                FloatingActionButton(
                    onClick = { onNavigateToCreateTask(projectId) },
                    containerColor = Color.Black.copy(alpha = 0.8f),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Task")
                }
            }
        }
    }
}


@Composable
private fun ProjectHeader(
    project: ProjectResponse?,
    taskCount: Int,
    loggedInUserId: Long?,
    projectHasPoll: Boolean,
    onNavigateBack: () -> Unit,
    onDeleteProject: () -> Unit,
    onAddAssignees: () -> Unit,
    onNavigateToCreatePoll: () -> Unit,
    onViewPollClicked: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        color = Color.White,
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .padding(top = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black.copy(0.8f))
                }

                Text(
                    "Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                if (projectHasPoll) {
                    IconButton(onClick = onViewPollClicked) {
                        Icon(Icons.Filled.Poll, contentDescription = "View Poll", tint = Color.Blue.copy(0.4f))
                    }
                }
                IconButton(onClick = onNavigateToChat) {
                    Icon(
                        imageVector = Icons.Filled.MarkUnreadChatAlt,
                        contentDescription = "Open Chat",
                        tint = Color.Black.copy(0.8f)
                    )
                }
                IconButton(onClick = onNavigateToAnalytics) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = "Project Analytics",
                        tint = Color.Black.copy(0.8f)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options",tint = Color.Black.copy(0.8f))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(if (projectHasPoll) "Create new Poll" else "Create Poll")
                            },
                            onClick = {
                                onNavigateToCreatePoll()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Create,
                                    contentDescription = ""
                                )
                            }
                        )
                        HorizontalDivider()

                        if (loggedInUserId != null && loggedInUserId == project?.createdBy) {
                            DropdownMenuItem(
                                text = { Text("Delete Project", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onDeleteProject()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "",
                                        tint = Color.Red
                                    )
                                }
                            )
                        }
                    }

                }
            }
            Spacer(modifier = Modifier.height(1.dp))
            if (project != null) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$taskCount tasks | ${com.example.linkit.util.TimeUtils.formatProjectDate(project.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                AssigneesRow(
                    assignees = project.assignees,
                    onAddAssigneeClick = onAddAssignees
                )
            }
        }
    }
}


@Composable
private fun AssigneesRow(
    assignees: List<ProjectAssigneeResponse>,
    onAddAssigneeClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-12).dp)
    ) {
        assignees.take(3).forEach { assignee ->
            AssigneeAvatar(assignee = assignee, size = 40.dp)
        }
        if (assignees.size > 3) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp,Color.White,CircleShape)
                    .background(Color.Black.copy(0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${assignees.size - 3}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 20.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDF1F3))
                .clickable { onAddAssigneeClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Assignee", tint = Color.Black)
        }
    }
}

@Composable
private fun TaskCard(
    tasks: List<TaskResponse>,
    attachmentsByTask: Map<Long, List<TaskAttachmentResponse>>,
    loggedInUserId: Long?,
    onAssigneeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel,
    onTaskClick: (TaskResponse) -> Unit
) {
    val sortedTasks = remember(tasks) {
        tasks.sortedByDescending { it.createdAt }
    }
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
    ) {
        itemsIndexed(
            items = sortedTasks,
            key = { _, task -> task.id }
        ) { index, task ->

            val currentTaskLocalDate = remember(task.createdAt) {
                com.example.linkit.util.TimeUtils.safeInstant(task.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            }

            val isFirstInGroup = if (index == 0) {
                true
            } else {
                val prevTask = sortedTasks[index - 1]
                val prevTaskLocalDate = com.example.linkit.util.TimeUtils.safeInstant(prevTask.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                currentTaskLocalDate != prevTaskLocalDate
            }

            val isLastInGroup = if (index == sortedTasks.lastIndex) {
                true
            } else {
                val nextTask = sortedTasks[index + 1]
                val nextTaskLocalDate = com.example.linkit.util.TimeUtils.safeInstant(nextTask.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                currentTaskLocalDate != nextTaskLocalDate
            }

            val previousStatus = if (index > 0) TaskStatus.valueOf(sortedTasks[index - 1].status) else null
            val attachments = attachmentsByTask[task.id] ?: emptyList()
            var cardSize by remember { mutableStateOf(IntSize.Zero) }

            Column {
                if (isFirstInGroup) {
                    Text(
                        text = currentTaskLocalDate.format(DateTimeFormatter.ofPattern("dd MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 32.dp, bottom = 12.dp, top = if (index > 0) 16.dp else 0.dp)
                    )
                }

                Row {
                    Timeline(
                        modifier = Modifier.height(with(LocalDensity.current) { cardSize.height.toDp() }),
                        status = TaskStatus.valueOf(task.status),
                        previousStatus = previousStatus,
                        isFirstInGroup = isFirstInGroup,
                        isLastInGroup = isLastInGroup
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TimelineTaskCard(
                        modifier = Modifier.onSizeChanged { cardSize = it },
                        task = task,
                        attachments = attachments,
                        loggedInUserId = loggedInUserId,
                        onAssigneeClick = onAssigneeClick,
                        onStatusChange = { newStatus ->
                            viewModel.updateTaskStatus(task.id, newStatus)
                        },
                        onUploadFile = { uri, fileName ->
                            viewModel.uploadAttachment(task.id, uri, fileName)
                        },
                        onViewAttachment = { attachment ->
                            viewModel.viewAttachment(context, attachment)
                        },
                        onDeleteTask = { viewModel.deleteTask(task.id) },
                        onTaskClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun Timeline(
    modifier: Modifier = Modifier,
    status: TaskStatus,
    previousStatus: TaskStatus?,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
) {
    val statusColor = when (status) {
        TaskStatus.TODO -> Color.Black.copy(0.3f)
        TaskStatus.IN_PROGRESS -> Color.Black.copy(0.7f)
        TaskStatus.COMPLETED -> Color.Black.copy(0.8f)
    }

    val previousStatusColor = when (previousStatus) {
        TaskStatus.TODO -> Color.White
        TaskStatus.IN_PROGRESS -> Color.White
        TaskStatus.COMPLETED -> Color.White
        null -> Color.Transparent
    }

    val density = LocalDensity.current
    val strokeWidth = with(density) { 2.dp.toPx() }
    val dashedPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    Box(
        modifier = modifier.width(32.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotRadius = 6.dp.toPx()
            val dotCenterY = 16.dp.toPx()

            if (!isFirstInGroup) {
                drawLine(
                    color = previousStatusColor,
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, dotCenterY),
                    strokeWidth = strokeWidth
                )
            }

            val bottomLineColor = if (isLastInGroup) Color.LightGray else statusColor
            val pathEffect = if (isLastInGroup) dashedPathEffect else null
            drawLine(
                color = bottomLineColor,
                start = Offset(center.x, dotCenterY),
                end = Offset(center.x, size.height),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )

            drawCircle(
                color = statusColor,
                radius = dotRadius,
                center = Offset(center.x, dotCenterY)
            )
        }
    }
}

@Composable
private fun TimelineTaskCard(
    modifier: Modifier = Modifier,
    task: TaskResponse,
    attachments: List<TaskAttachmentResponse>,
    loggedInUserId: Long?,
    onAssigneeClick: (Long) -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onUploadFile: (Uri, String) -> Unit,
    onViewAttachment: (TaskAttachmentResponse) -> Unit,
    onDeleteTask: () -> Unit,
    onTaskClick: (TaskResponse) -> Unit
) {
    val context = LocalContext.current
    val status = TaskStatus.valueOf(task.status)
    val statusColor = when (status) {
        TaskStatus.TODO -> Color.Black
        TaskStatus.IN_PROGRESS -> Color.Black
        TaskStatus.COMPLETED -> Color.Black
    }

    val canChangeStatus = loggedInUserId == task.assignee.userId
    var showStatusMenu by remember { mutableStateOf(false) }
    val canDeleteTask = loggedInUserId == task.createdBy
    var showDeleteDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it)
            onUploadFile(it, fileName)
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTask()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }


    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onTaskClick(task) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    val statusModifier = if (canChangeStatus) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showStatusMenu = true }
                            .padding(2.5.dp)
                    } else {
                        Modifier.padding(5.dp)
                    }

                    Surface(
                        color = Color.Red.copy(alpha = 0.5f),
                        shape= BrushStrokeShape(
                            brushSide = BrushStrokeShape.Side.Right,
                            jaggedness = 30f,
                            cornerRadius = 8f,
                            variation = BrushStrokeShape.BrushVariation.WAVED
                        ),
                        modifier = Modifier.padding(3.dp)
                    ) {
                        Row(
                            modifier = statusModifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = status.displayName,
                                color = statusColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (canChangeStatus) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Change Status",
                                    tint = statusColor
                                )
                            }
                        }

                        if (canChangeStatus) {
                            DropdownMenu(
                                expanded = showStatusMenu,
                                onDismissRequest = { showStatusMenu = false }) {
                                TaskStatus.entries.forEach { statusOption ->
                                    DropdownMenuItem(
                                        text = { Text(statusOption.displayName) },
                                        onClick = {
                                            onStatusChange(statusOption)
                                            showStatusMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                if (canDeleteTask) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Delete Task",
                            tint = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = grenze_blackItalic)

            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.height(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(attachments) { attachment ->
                        AttachmentChip(attachment = attachment, onViewFile = {
                            onViewAttachment(attachment)
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CountIndicator(icon = Icons.Filled.AttachFile, count = task.attachmentCount)
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }, modifier = Modifier.size(21.dp)) {
                        Icon(Icons.Filled.PostAdd, contentDescription = "Add Attachment", tint = Color.Black.copy(0.5f))
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    AssigneeAvatar(
                        assignee = task.assignee,
                        size = 32.dp,
                        onClick = { onAssigneeClick(task.assignee.userId) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Assigned By ${task.creator.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}



private fun getFileName(context: Context, uri: Uri): String {
    var fileName = "unknown_file"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex)
            }
        }
    }
    return fileName
}

@Composable
fun AttachmentChip(
    attachment: TaskAttachmentResponse,
    onViewFile: (TaskAttachmentResponse) -> Unit
) {
    OutlinedButton(
        onClick = { onViewFile(attachment) },
        modifier = Modifier.widthIn(max = 150.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.OpenInNew,
            contentDescription = "View Attachment",
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(attachment.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CountIndicator(icon: ImageVector, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun AssigneeAvatar(
    assignee: ProjectAssigneeResponse,
    size: Dp,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .size(size)
            .border(
                width = 2.dp,
                color = Color.White,
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(clickableModifier)
    ) {
        if (!assignee.profileImageUrl.isNullOrBlank()) {

            AuthorizedAsyncImage(
                imageUrl = assignee.profileImageUrl,
                contentDescription = assignee.name,
                isOffline = isOffline,
                cacheKey = assignee.userId.toString(),
                modifier = Modifier.fillMaxSize()
            )

        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = assignee.name,
                modifier = Modifier.fillMaxSize().padding(6.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyTasksView(modifier: Modifier = Modifier, onCreateTask: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_project),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Black.copy(0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "No Tasks Yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Create the first task for this project.")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateTask, colors = ButtonDefaults.buttonColors(Color.Black.copy(alpha = 0.8f))) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Task", color = Color.White)
        }
    }
}

@Composable
private fun DeleteProjectConfirmationDialog(projectName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Project") },
        text = { Text("Are you sure you want to permanently delete '$projectName'? All of its tasks and data will be lost forever.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(dateString: String, pattern: String = "MMM dd, yyyy"): String {
    return try {
        val date = if (dateString.contains("T")) {
            val dateTime = LocalDateTime.parse(dateString.removeSuffix("Z").replace(" ", "T"))
            dateTime.toLocalDate()
        } else {
            LocalDate.parse(dateString)
        }
        date.format(DateTimeFormatter.ofPattern(pattern))
    } catch (e: Exception) {
        dateString
    }
}

@Composable
private fun AssigneeSelectionDialog(
    assignees: List<UserSearchResult>,
    selectedAssignees: List<ProjectAssigneeResponse>,
    onAssigneeSelected: (UserSearchResult) -> Unit,
    onAssigneeDeselected: (UserSearchResult) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Update Assignees",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assignees) { assignee ->
                        val isSelected = selectedAssignees.any { it.userId == assignee.userId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        onAssigneeDeselected(assignee)
                                    } else {
                                        onAssigneeSelected(assignee)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    Color.Red.copy(0.4f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssigneeAvatar(
                                    assignee = ProjectAssigneeResponse(assignee.userId, assignee.name, assignee.profileImageUrl),
                                    size = 40.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = assignee.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSave) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserProfileSheet(
    state: ViewedProfileState,
    onClose: () -> Unit
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.Black)
        }
        return
    }

    val profile = state.profile ?: run {
        Box(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("User not found.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("About", "Connections")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                AuthorizedAsyncImage(
                    imageUrl = profile.profileImageUrl,
                    contentDescription = profile.name,
                    cacheKey = profile.userId.toString(),
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color(0xFFF1F1F1), CircleShape)
                )

                Surface(
                    modifier = Modifier.size(24.dp).offset(x = (-4).dp, y = (-4).dp),
                    color = Color(0xFF32BF94),
                    shape = CircleShape,
                    border = BorderStroke(3.dp, Color.White)
                ) {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = profile.name ?: "Unknown User",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = odisansFamily,
                color = Color.Black
            )

            if (!profile.jobTitle.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = profile.jobTitle,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = Color.Black,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = Color.Black,
                            height = 3.dp
                        )
                    }
                },
                divider = { HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color(0xFFF8F9FA))
            ) { page ->
                when (page) {
                    0 -> AboutTab(profile.aboutMe)
                    1 -> ConnectionsTab(state.connections)
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color(0xFFF1F1F1), CircleShape)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                modifier = Modifier.size(18.dp),
                tint = Color.Black
            )
        }
    }
}

@Composable
fun AboutTab(aboutMe: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Bio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = if (!aboutMe.isNullOrBlank()) aboutMe else "No information provided.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = if (aboutMe.isNullOrBlank()) Color.Gray else Color.Black,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
fun ConnectionsTab(connections: List<ConnectionResponse>) {
    if (connections.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No connections yet", color = Color.Gray)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(connections) { connection ->
            ConnectionCard(connection = connection)
        }
    }
}

@Composable
fun ConnectionCard(connection: ConnectionResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthorizedAsyncImage(
                imageUrl = connection.profileImageUrl,
                contentDescription = connection.name,
                cacheKey = connection.userId.toString(),
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F1F1))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!connection.company.isNullOrBlank()) {
                    Text(
                        text = connection.company,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}
@Composable
private fun PollDialog(
    poll: PollResponse,
    loggedInUserId: Long?,
    onDismissRequest: () -> Unit,
    onVote: (pollId: Long, optionId: Long) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Poll,
                        contentDescription = "Close",
                        tint = Color.Black.copy(0.8f)
                    )
                    Text(
                        text = "Poll",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "Close",
                            tint = Color.Black.copy(0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = poll.question,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                poll.options.forEach { option ->
                    val isSelected = option.votes.any { it.user.userId == loggedInUserId }
                    PollOptionItem(
                        poll = poll,
                        option = option,
                        isSelected = isSelected,
                        onVote = { onVote(poll.id, option.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PollOptionItem(
    poll: PollResponse,
    option: PollOptionResponse,
    isSelected: Boolean,
    onVote: () -> Unit
) {
    val progress = if (poll.totalVotes > 0) {
        option.voteCount.toFloat() / poll.totalVotes.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onVote
            )
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Color.Black.copy(alpha = 0.8f) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = option.optionText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else Color.Gray
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp)
                .height(6.dp)
                .clip(CircleShape),
            color = if (isSelected) Color.Red.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${option.voteCount} votes",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                    option.votes.take(5).forEach { vote ->
                        AuthorizedAsyncImage(
                            imageUrl = vote.user.profileImageUrl,
                            contentDescription = null,
                            cacheKey = vote.user.userId.toString(),
                            modifier = Modifier
                                .size(25.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.Black.copy(0.8f), CircleShape)
                        )
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(
    task: TaskResponse,
    onDismiss: () -> Unit
) {

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.32f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 45.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                        .padding(top = 60.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = Color.Blue.copy(alpha = 0.2f),
                            shape = BrushStrokeShape(brushSide = BrushStrokeShape.Side.Right, jaggedness = 15f),
                        ) {
                            Text(
                                text = task.status,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(13.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(13.dp))

                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = task.description ?: "No description provided.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))



                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        DateInfoColumn(label = "Created At", date = task.createdAt)
                        DateInfoColumn(label = "Deadline", date = task.endDate ?: "TBD")
                    }
                }
            }
         Row(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy((-15).dp)
            ) {
                AssigneeAvatar(
                    assignee = ProjectAssigneeResponse(
                        userId = task.createdBy,
                        name = task.creator.name,
                        profileImageUrl = task.creator.profileImageUrl
                    ),
                    size = 90.dp
                )
                AssigneeAvatar(
                    assignee = task.assignee,
                    size = 90.dp
                )
            }
        }
    }
}



@Composable
private fun DateInfoColumn(label: String, date: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(
            text = formatDate(date, "MMM dd, yyyy"),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

