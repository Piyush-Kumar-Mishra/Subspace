package com.example.linkit.view.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.linkit.R
import com.example.linkit.data.models.ConnectionResponse
import com.example.linkit.data.models.ProjectAssigneeResponse
import com.example.linkit.data.models.ProjectResponse
import com.example.linkit.data.models.TaskResponse
import com.example.linkit.data.models.TaskStatus
import com.example.linkit.data.models.UserSearchResult
import com.example.linkit.util.Constants
import com.example.linkit.util.UiEvent
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
    viewModel: ProjectViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCreateTask: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val createProjectState by viewModel.createProjectState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState()}
    val viewedProfileState by profileViewModel.viewedProfileState.collectAsState()
     val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showBottomSheet = remember { derivedStateOf { viewedProfileState.profile != null } }



    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { profileViewModel.closeUserProfileSheet() },
            sheetState = sheetState,
            scrimColor = Color.Black.copy(alpha = 0.5f),
        ) {
            UserProfileSheetContent(
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
        color = Color(0xFFF7F8FA)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProjectHeader(
                project = uiState.currentProject,
                taskCount = uiState.tasks.size,
                onNavigateBack = {
                    viewModel.goBackToProjects()
                    onNavigateBack()
                },
                onDeleteProject = { viewModel.onDeleteProjectClicked() },
                onAddAssignees = {
                    viewModel.loadProjectForEditing(projectId, openAssigneeDialog = true)
                }
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
                TaskTimeline(
                    tasks = uiState.tasks,
                    project = uiState.currentProject,
                    onAssigneeClick = { userId ->
                        profileViewModel.viewUserProfile(userId)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }



        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (uiState.currentProject != null) {
                FloatingActionButton(
                    onClick = { onNavigateToCreateTask(projectId) },
                    containerColor = MaterialTheme.colorScheme.primary,
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
    onNavigateBack: () -> Unit,
    onDeleteProject: () -> Unit,
    onAddAssignees: () -> Unit
) {
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
                .padding(16.dp)
                .padding(top = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("OMP Dashboard", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDeleteProject) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Project",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (project != null) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$taskCount tasks | ${formatDate(project.startDate, "MMM dd, yyyy")}",
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${assignees.size - 3}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 20.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF7F8FA))
                .clickable { onAddAssigneeClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Assignee", tint = Color.Gray)
        }
    }
}

@Composable
private fun TaskTimeline(
    tasks: List<TaskResponse>,
    project: ProjectResponse?,
    onAssigneeClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedTasks = remember(tasks) {
        tasks.sortedByDescending { it.createdAt }
    }

    val creator = remember(project) {
        project?.assignees?.find { it.userId == project.createdBy }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
    ) {
        itemsIndexed(sortedTasks) { index, task ->
            val date = LocalDate.parse(task.createdAt.take(10))

            val isFirstInGroup = index == 0 || date != LocalDate.parse(sortedTasks[index - 1].createdAt.take(10))
            val isLastInGroup = index == sortedTasks.lastIndex || date != LocalDate.parse(sortedTasks[index + 1].createdAt.take(10))
            val previousStatus = if (index > 0) TaskStatus.valueOf(sortedTasks[index - 1].status) else null

            val taskCreator = remember(project, task) {
                project?.assignees?.find { it.userId == task.createdBy }
            }

            Column {
                if (isFirstInGroup) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("dd MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 32.dp, bottom = 12.dp, top = if (index > 0) 16.dp else 0.dp)
                    )
                }

                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    TimelineGutter(
                        status = TaskStatus.valueOf(task.status),
                        previousStatus = previousStatus,
                        isFirstInGroup = isFirstInGroup,
                        isLastInGroup = isLastInGroup
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TimelineTaskCard(
                        task = task,
                        creator= taskCreator,
                        onAssigneeClick = onAssigneeClick
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineGutter(
    status: TaskStatus,
    previousStatus: TaskStatus?,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
) {
    val statusColor = when (status) {
        TaskStatus.TODO -> Color.Gray
        TaskStatus.IN_PROGRESS -> Color(0xFFFFA000)
        TaskStatus.COMPLETED -> Color(0xFF673AB7)
    }

    val previousStatusColor = when (previousStatus) {
        TaskStatus.TODO -> Color.Gray
        TaskStatus.IN_PROGRESS -> Color(0xFFFFA000)
        TaskStatus.COMPLETED -> Color(0xFF673AB7)
        null -> Color.Transparent
    }

    val density = LocalDensity.current
    val strokeWidth = with(density) { 2.dp.toPx() }
    val dashedPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    Box(
        modifier = Modifier
            .width(32.dp)
            .fillMaxHeight()
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
    task: TaskResponse,
    creator: ProjectAssigneeResponse?,
    onAssigneeClick: (Long) -> Unit

) {
    val status = TaskStatus.valueOf(task.status)
    val statusColor = when (status) {
        TaskStatus.TODO -> Color.Gray
        TaskStatus.IN_PROGRESS -> Color(0xFFFFA000)
        TaskStatus.COMPLETED -> Color(0xFF673AB7)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = status.displayName,
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Nº ${task.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CountIndicator(icon = Icons.AutoMirrored.Filled.ArrowBack, count = task.messageCount)
                    CountIndicator(icon = Icons.Filled.Check, count = task.attachmentCount)
                }
                Spacer(modifier = Modifier.weight(1f))
                AssigneeAvatar(
                    assignee = task.assignee,
                    size = 32.dp,
                    onClick = { onAssigneeClick(task.assignee.userId) }
                )

                if (creator != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Created By ${creator.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

            }
        }
    }
}

@Composable
private fun CountIndicator(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun AssigneeAvatar(assignee: ProjectAssigneeResponse, size: androidx.compose.ui.unit.Dp, onClick: (() -> Unit)? = null) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(clickableModifier)
    ) {
        if (!assignee.profileImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = "${Constants.BASE_URL}${assignee.profileImageUrl.removePrefix("/")}",
                contentDescription = assignee.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_person),
                error = painterResource(R.drawable.ic_person)
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "No Tasks Yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Create the first task for this project.")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateTask) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Task")
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
                                    MaterialTheme.colorScheme.primaryContainer
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
fun UserProfileSheetContent(
    state: ViewedProfileState,
    onClose: () -> Unit
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val profile = state.profile
    if (profile == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("User not found.")
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("About", "Connections")


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            AsyncImage(
                model = "${Constants.BASE_URL}${profile.profileImageUrl?.removePrefix("/")}",
                contentDescription = profile.name ?: "User Avatar",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_person),
                error = painterResource(R.drawable.ic_person)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(profile.name ?: "Unknown User", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!profile.jobTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(profile.jobTitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { /*TODO*/ }, modifier = Modifier.height(48.dp).width(150.dp)) {
                    Text("MESSAGE")
                }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Connected")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(text = title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.height(250.dp)
            ) { page ->
                when (page) {
                    0 -> AboutTab(profile.aboutMe)
                    1 -> ConnectionsTab(state.connections)
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
fun AboutTab(aboutMe: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!aboutMe.isNullOrBlank()) {
            Text(aboutMe, style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("No information provided.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
    }
}

@Composable
fun ConnectionsTab(connections: List<ConnectionResponse>) {
    if (connections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No connections to show.")
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "${Constants.BASE_URL}${connection.profileImageUrl?.removePrefix("/")}",
                contentDescription = connection.name ?: "User Avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_person),
                error = painterResource(R.drawable.ic_person)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(connection.name ?: "Unknown User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!connection.company.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(connection.company, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }
    }
}
