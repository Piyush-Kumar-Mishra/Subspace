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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.linkit.R
import com.example.linkit.data.models.ProjectAssigneeResponse
import com.example.linkit.data.models.ProjectResponse
import com.example.linkit.util.Constants
import com.example.linkit.util.UiEvent
import com.example.linkit.viewmodel.ProjectFilter
import com.example.linkit.viewmodel.ProjectViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.flow.first

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
        viewModel.loadProjects()
        viewModel.uiEvent.collectLatest { event ->
            if (event is UiEvent.ShowToast) snackbarHost.showSnackbar(event.msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Subspace") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateProject,
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.Black.copy(alpha = 0.8f),
                contentColor = Color.White
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
                Calendar(
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                item {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        FilterChips(
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
                        ProjectCard(
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
fun Calendar(
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

    var hasAutoScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(currentMonth) {
        val isTodayMonth =
            currentMonth.month == today.month &&
                    currentMonth.year == today.year

        if (isTodayMonth && !hasAutoScrolled) {
            hasAutoScrolled = true

            snapshotFlow { listState.layoutInfo.viewportSize.width }
                .first { it > 0 }

            val itemWidthPx = with(density) { 62.dp.toPx() }
            val centerOffset =
                (listState.layoutInfo.viewportSize.width / 2) - (itemWidthPx / 2)

            listState.animateScrollToItem(
                index = today.dayOfMonth - 1,
                scrollOffset = -centerOffset.toInt()
            )
        }
    }


    Column() {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.8f)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onPrev) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = Color.White)
            }

            Text(
                "${
                    currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
                } ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
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

                        Spacer(Modifier.height(2.dp))

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    if (isSelected) onDateClicked(null)
                                    else onDateClicked(date)
                                },
                            shape = CircleShape,
                            color = if (isSelected)
                                Color.Black.copy(alpha = 0.8f)
                            else
                                Color.Transparent,
                            border = if (isToday && !isSelected)
                                BorderStroke(2.dp,Color.Black.copy(alpha = 0.8f))
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
fun FilterChips(
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

    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(filters) { (filter, label) ->

            val sel = selected == filter

            FilterChip(
                selected = sel,
                onClick = { onSelected(filter) },
                label = {
                    Text(
                        label,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel)
                            Color.White
                        else
                            Color.Black

                    )
                },
                shape = RoundedCornerShape(10.dp),

                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = Color.Black,
                    selectedContainerColor = Color.Black.copy(alpha = 0.8f),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun UserAvatar(assignees: List<ProjectAssigneeResponse>) {
    val visible = assignees.take(3)
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        visible.forEach { user ->
            AsyncImage(
                model = Constants.BASE_URL + (user.profileImageUrl ?: ""),
                contentDescription = null,
                modifier = Modifier
                    .size(29.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_person)
            )
        }
        if (assignees.size > 3) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F2937))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+${assignees.size - 3}", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectResponse,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onAnalytics: (Long) -> Unit
) {
    val startDate = formatDateFromIso(project.createdAt)
    val endDate = formatDateFromIso(project.endDate)
    val startTime = formatTimeFromIso(project.createdAt)
    val endTime = formatTimeFromIso(project.endDate)

    val (bgColor) = when (project.priority.uppercase()) {
        "HIGH" -> Color(0xFFFFFFFF) to Color(0xFF673AB7)
        "MEDIUM" -> Color(0xFFFFFFFF) to Color(0xFF0288D1)
        "LOW" -> Color(0xFFFFFFFF) to Color(0xFF388E3C)
        else -> Color(0xFFFFE0B2) to Color(0xFFF57C00)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 10.dp)
            .height(IntrinsicSize.Min)
    ) {

        Column(
            modifier = Modifier
                .width(35.dp)
                .fillMaxHeight(),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(startTime, style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Gray))

                Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                ) {
                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = Color.Gray,
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height),
                        pathEffect = pathEffect,
                        strokeWidth = 2f
                    )
                }

                Box(Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Gray))
            }

            Text(endTime, style = MaterialTheme.typography.labelSmall, color = Color.Black)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 210.dp)
                .clickable(onClick = onClick)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
                    .shadow(
                        elevation = 5.dp,
                        shape = TabbedCardShape(20.dp, 193.dp, 50.dp),
                    )
                    .offset(x = 2.dp, y = 4.dp),
                shape = TabbedCardShape(21.dp, 180.dp, 50.dp),
                color = bgColor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 44.dp)
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1F2937)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_project),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Surface(
                            color = Color.Blue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = project.priority.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$startDate - $endDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.KeyboardDoubleArrowRight,
                                null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = project.description ?: "No description",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = 0.5f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(10.dp))


                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )

                        Spacer(Modifier.height(10.dp))

                        if (project.tags.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),

                            ) {
                                project.tags.forEach { tag ->
                                    Surface(
                                        color = Color.Red.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color.Black)
                                    ) {
                                        Text(
                                            tag,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "${project.taskCount} Tasks",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 25.dp, end = 7.dp),
                horizontalAlignment = Alignment.End
            ) {

                if (project.assignees.isNotEmpty()) {
                    Box(modifier = Modifier.scale(1.2f)) {
                        UserAvatar(project.assignees)
                    }
                }

                Spacer(Modifier.height(38.dp))

                Box(modifier = Modifier.offset(x = (-10).dp)) {
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, "Menu", tint = Color.DarkGray)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { onEdit(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Analytics") },
                            onClick = { onAnalytics(project.id); showMenu = false }
                        )
                    }
                }
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
        Icon(painterResource(R.drawable.ic_project), null, Modifier.size(80.dp), tint = Color.Black.copy(alpha = 0.8f))

        Spacer(Modifier.height(12.dp))
        Text("No Projects Yet")
        Text("Create your first project to get started")
        Spacer(Modifier.height(18.dp))

        Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(Color.Black.copy(alpha = 0.8f))) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Create Project")
        }
    }
}


fun formatTimeFromIso(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val localTime = instant
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
        localTime.format(java.time.format.DateTimeFormatter.ofPattern("hh a"))
    } catch (e: Exception) {
        "$e"
    }
}

fun formatDateFromIso(iso: String?): String {
    if (iso.isNullOrBlank()) return "--"
    return try {
        val instant = java.time.Instant.parse(iso)
        instant.atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))
    } catch (e: Exception) {
        "$e"
    }
}

class TabbedCardShape(
    private val cornerRadius: Dp = 20.dp,
    private val tabWidth: Dp = 150.dp,
    private val cutoutHeight: Dp = 50.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = with(density) { cornerRadius.toPx() }
        val rawTabW = with(density) { tabWidth.toPx() }
        val cutH = with(density) { cutoutHeight.toPx() }
        val w = size.width
        val h = size.height

        val tabW = rawTabW.coerceAtMost(w - (radius * 3))

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, radius)
            quadraticBezierTo(0f, 0f, radius, 0f)
            lineTo(tabW, 0f)
            cubicTo(
                tabW + radius, 0f,
                tabW + radius, cutH,
                tabW + (radius * 2), cutH
            )
            lineTo(w - radius, cutH)
            quadraticBezierTo(w, cutH, w, cutH + radius)
            lineTo(w, h - radius)
            quadraticBezierTo(w, h, w - radius, h)
            lineTo(radius, h)
            quadraticBezierTo(0f, h, 0f, h - radius)
            close()
        }
        return Outline.Generic(path)
    }
}