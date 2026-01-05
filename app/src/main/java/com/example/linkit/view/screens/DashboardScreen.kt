package com.example.linkit.view.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.linkit.R
import com.example.linkit.data.models.ProjectAssigneeResponse
import com.example.linkit.data.models.ProjectPriority
import com.example.linkit.data.models.ProjectResponse
import com.example.linkit.util.Constants
import com.example.linkit.util.TimeUtils
import com.example.linkit.util.UiEvent
import com.example.linkit.viewmodel.ProjectFilter
import com.example.linkit.viewmodel.ProjectViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ProjectViewModel = hiltViewModel(),
    onNavigateToCreateProject: () -> Unit,
    onNavigateToTaskScreen: (Long) -> Unit,
    onNavigateToEditProject: (Long) -> Unit,
    onNavigateToAnalytics: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val projectListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            if (event is UiEvent.ShowToast) snackbarHost.showSnackbar(event.msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("My Projects") },
                actions = {
                    IconButton(onClick = { viewModel.loadProjects() }) {
                        Icon(
                            painterResource(R.drawable.ic_refresh),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateProject,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, null)
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {

        Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F5F7))
            ) {
                AnimatedCalendar(
                    selectedDate = uiState.selectedDate,
                    currentMonth = uiState.currentMonth,
                    daysWithProjects = uiState.daysWithProjectsInMonth,
                    onNext = viewModel::goToNextMonth,
                    onPrev = viewModel::goToPreviousMonth,
                    onDateClicked = viewModel::onDateSelected
                )
            }

            LazyColumn(
                state = projectListState,
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FilterChipsRow(
                            selected = uiState.selectedFilter,
                            onSelected = {
                                viewModel.clearSelectedDate()
                                viewModel.applyFilter(it)
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }

                if (uiState.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth(), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (uiState.projects.isEmpty()) {
                    item {
                        EmptyProjectsView(onNavigateToCreateProject)
                    }
                } else {
                    items(uiState.projects) { project ->
                        ProjectCardItem(
                            project = project,
                            onClick = {
                                viewModel.selectProject(project)
                                onNavigateToTaskScreen(project.id)
                            },
                            onEdit = { onNavigateToEditProject(project.id) },
                            onAnalytics = { projectId -> onNavigateToAnalytics(projectId) }
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(end = 14.dp, bottom = 110.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = projectListState.firstVisibleItemIndex > 1
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                projectListState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, null)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedCalendar(
    selectedDate: LocalDate?,
    currentMonth: YearMonth,
    daysWithProjects: Set<Int>,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onDateClicked: (LocalDate?) -> Unit
) {
    val today = LocalDate.now()
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentMonth) {
        if (currentMonth.month == today.month && currentMonth.year == today.year) {
            val index = today.dayOfMonth - 1
            scope.launch {

                val itemWidthPx = with(density) { 62.dp.toPx() }

                val centerOffset =
                    (listState.layoutInfo.viewportSize.width / 2) - (itemWidthPx / 2)
                listState.scrollToItem(index, -centerOffset.toInt())
            }
        }
    }

    Column(
        Modifier
            .padding(
                horizontal = 14.dp,
                vertical = 14.dp
            )
    ) {

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }

            Text(
                "${
                    currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
                } ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Spacer(Modifier.height(6.dp))

        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            }
        ) { month ->

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {

                items(month.lengthOfMonth()) { index ->

                    val day = index + 1
                    val date = month.atDay(day)
                    val isToday = date == today
                    val isSelected = date == selectedDate

                    val scale by animateFloatAsState(
                        if (isSelected) 1.12f else 1f,
                        tween(200)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(scale)
                    ) {

                        Text(
                            date.dayOfWeek.name.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )

                        Spacer(Modifier.height(6.dp))

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    if (isSelected) onDateClicked(null)
                                    else onDateClicked(date)
                                },
                            shape = CircleShape,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent,
                            border = if (isToday && !isSelected)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    day.toString(),
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else Color.Black,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        if (daysWithProjects.contains(day)) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selected: ProjectFilter,
    onSelected: (ProjectFilter) -> Unit
) {
    val filters = listOf(
        ProjectFilter.ALL to "All",
        ProjectFilter.TODAY to "Today",
        ProjectFilter.HIGH to "High",
        ProjectFilter.MEDIUM to "Medium",
        ProjectFilter.LOW to "Low"
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filters) { (filter, label) ->

            val sel = selected == filter

            FilterChip(
                selected = sel,
                onClick = { onSelected(filter) },
                label = {
                    Text(
                        label,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (sel)
                        MaterialTheme.colorScheme.primary else Color.White,
                    labelColor = if (sel)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        Color.Black
                )
            )
        }
    }
}

@Composable
fun ProjectCardItem(
    project: ProjectResponse,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onAnalytics: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Column(Modifier.padding(14.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(Modifier.weight(1f)) {
                    Text(
                        project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!project.description.isNullOrBlank()) {
                        Text(
                            project.description,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(ProjectPriority.valueOf(project.priority))
                    Spacer(Modifier.width(8.dp))

                    var showMenu by remember { mutableStateOf(false) }

                    // Fix: Add wrapContentSize to ensure proper anchoring for DropdownMenu
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    onEdit() // Trigger action first
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Analytics") },
                                onClick = {
                                    onAnalytics(project.id) // Trigger action first
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Start Date", style = MaterialTheme.typography.labelSmall)
                    Text(TimeUtils.formatProjectDate(project.startDate))
                }

                Text("${project.taskCount} tasks")
            }

            if (project.tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyRow {
                    items(project.tags) { TagChip(it) }
                }
            }
            Spacer(Modifier.height(10.dp))
            AssigneesRow(project.assignees)
        }
    }
}

@Composable
fun PriorityBadge(priority: ProjectPriority) {
    val (color, bg) =
        when (priority) {
            ProjectPriority.LOW -> Color(0xFF4CAF50) to Color(0x334CAF50)
            ProjectPriority.MEDIUM -> Color(0xFFFF9800) to Color(0x33FF9800)
            ProjectPriority.HIGH -> Color(0xFFF44336) to Color(0x33F44336)
        }

    Surface(color = bg, shape = RoundedCornerShape(14.dp)) {
        Text(priority.displayName, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color)
    }
}

@Composable
fun TagChip(tag: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(tag, Modifier.padding(6.dp))
    }
}

@Composable
fun AssigneesRow(assignees: List<ProjectAssigneeResponse>) {

    val avatarSize = 34.dp
    val overlapOffset = (-12).dp

    Row(
        modifier = Modifier.height(avatarSize),
        verticalAlignment = Alignment.CenterVertically
    ) {

        assignees.take(4).forEachIndexed { index, assignee ->

            Box(
                modifier = Modifier
                    .offset(x = overlapOffset * index)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.White, CircleShape)
            ) {

                if (!assignee.profileImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = Constants.BASE_URL + assignee.profileImageUrl.removePrefix("/"),
                        contentDescription = assignee.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        val remaining = assignees.size - 4
        if (remaining > 0) {
            Box(
                modifier = Modifier
                    .offset(x = overlapOffset * 4)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+$remaining",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyProjectsView(onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(R.drawable.ic_project), null, Modifier.size(80.dp))

        Spacer(Modifier.height(12.dp))
        Text("No Projects Yet")
        Text("Create your first project to get started")
        Spacer(Modifier.height(18.dp))

        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Create Project")
        }
    }
}
