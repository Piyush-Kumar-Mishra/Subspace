package com.example.linkit.view.screens

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.linkit.data.models.*
import com.example.linkit.view.components.AuthorizedAsyncImage
import com.example.linkit.viewmodel.AnalyticsViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

object AnalyticsColors {
    val Blue = Color(0xFF2196F3)
    val Green = Color(0xFF4CAF50)
    val Orange = Color(0xFFFF9800)
    val Red = Color(0xFFF44336)
    val Purple = Color(0xFF673AB7)
    val Teal = Color(0xFF009688)
    val Background = Color(0xFFF5F7FA)
    val CardBg = Color.White
    val TextPrimary = Color(0xFF1A1C1E)
    val TextSecondary = Color(0xFF757575)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    projectId: Long,
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadAnalytics(projectId)
    }

    Scaffold(
        containerColor = AnalyticsColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Project Analytics",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(0.8f)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AnalyticsColors.CardBg
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = AnalyticsColors.CardBg,
                contentColor = Color.Black.copy(0.8f),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = Color.Black.copy(0.8f)
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Overview", fontWeight = FontWeight.Medium, color = Color.Black.copy(0.8f)) },
                    icon = { Icon(Icons.Filled.Code, null, tint = Color.Black.copy(0.8f)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Team Performance", fontWeight = FontWeight.Medium, color = Color.Black.copy(0.8f)) },
                    icon = { Icon(Icons.Filled.PeopleAlt, null, tint = Color.Black.copy(0.8f)) }
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {

                when (uiState.selectedTab) {
                    0 -> ProjectOverviewTab(uiState.summary, uiState.productivity)
                    1 -> TeamPerformanceTab(uiState.workload, uiState.assigneeStats)
                }
            }
        }
    }
}

@Composable
fun ProjectOverviewTab(
    summary: ProjectSummaryResponse?,
    productivity: List<TimeSeriesPointResponse>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                "Project Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(0.8f)
            )
        }


        item {
            summary?.let {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                    StatCard(
                        title = "Total Tasks",
                        count = it.totalTasks.toString(),
                        icon = Icons.AutoMirrored.Filled.List,
                        color = AnalyticsColors.Blue,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Overdue",
                        count = it.overdue.toString(),
                        icon = Icons.Default.Warning,
                        color = AnalyticsColors.Red,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                    StatCard(
                        title = "Completed",
                        count = it.completed.toString(),
                        icon = Icons.Default.CheckCircle,
                        color =  Color.Black.copy(0.8f),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "In Progress",
                        count = it.inProgress.toString(),
                        icon = Icons.Default.DateRange,
                        color = Color.Black.copy(0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            AnalyticsCard(title = "Task Distribution") {
                Box(Modifier.height(300.dp)) {
                    summary?.let { ProjectStatusDonutChart(it) }
                }
            }
        }

        item {
            AnalyticsCard(title = "Productivity Trends (Completed)") {
                Box(Modifier.height(300.dp)) {
                    ProductivityLineChart(productivity)
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = AnalyticsColors.TextSecondary,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                count,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AnalyticsColors.TextPrimary
            )
        }
    }
}

@Composable
fun AnalyticsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AnalyticsColors.TextPrimary
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = DividerDefaults.Thickness, color = Color.LightGray.copy(alpha = 0.4f)
            )
            content()
        }
    }
}
@Composable

fun TeamPerformanceTab(
    workload: List<AssigneeWorkloadResponse>,
    stats: List<AssigneeStatsResponse>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Workload Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(0.8f)
            )
        }

        item {
            AnalyticsCard(title = "Tasks per Member") {
                Box(Modifier.height(250.dp)) {
                    WorkloadBarChart(workload)
                }
            }
        }

        item {
            Text(
                "Detailed Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(stats) { userStat ->
            DetailedAssigneeItem(userStat)
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
fun DetailedAssigneeItem(stat: AssigneeStatsResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                AuthorizedAsyncImage(
                    imageUrl = stat.profileImageUrl,
                    contentDescription = "Profile Picture of ${stat.assigneeName}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    cacheKey = stat.userId?.toString(),
                    isOffline = false
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        stat.assigneeName ?: "Unknown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${stat.assigned} Tasks Assigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = AnalyticsColors.TextSecondary
                    )
                }

                val completionRate = if(stat.assigned > 0)
                    (stat.completed.toFloat() / stat.assigned.toFloat() * 100).toInt()
                else 0

                Badge(
                    containerColor = if(completionRate == 100) AnalyticsColors.Green else AnalyticsColors.Blue
                ) {
                    Text("$completionRate% Done", modifier = Modifier.padding(4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            val progress = if(stat.assigned > 0)
                stat.completed.toFloat() / stat.assigned.toFloat()
            else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = AnalyticsColors.Green,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
            )

            Spacer(Modifier.height(12.dp))


            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatChip("Completed: ${stat.completed}", AnalyticsColors.Green)
                StatChip("In Progress: ${stat.pending}", AnalyticsColors.Orange)
                StatChip("Overdue: ${stat.overdue}", AnalyticsColors.Red)
            }
        }
    }
}

@Composable
fun UserAvatar(name: String) {
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase()

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun StatChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = AnalyticsColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
fun ProjectStatusDonutChart(summary: ProjectSummaryResponse) {

    if (summary.totalTasks == 0L) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tasks available", color = Color.Gray)
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                legend.setDrawInside(false)

                // Donut
                isDrawHoleEnabled = true
                setHoleColor(AndroidColor.TRANSPARENT)
                setTransparentCircleColor(AndroidColor.WHITE)
                setTransparentCircleAlpha(110)
                holeRadius = 58f
                transparentCircleRadius = 61f
                setDrawCenterText(true)
                centerText = "Status"
                setCenterTextSize(16f)
                setCenterTextTypeface(Typeface.DEFAULT_BOLD)

                setEntryLabelColor(AndroidColor.WHITE)
                setEntryLabelTextSize(0f)
                setUsePercentValues(false)
                animateY(1400, Easing.EaseInOutQuad)
            }
        },
        update = { chart ->
            val entries = listOf(
                PieEntry(summary.completed.toFloat(), "Done"),
                PieEntry(summary.inProgress.toFloat(), "In Progress"),
                PieEntry(summary.todo.toFloat(), "To Do"),
                PieEntry(summary.overdue.toFloat(), "Overdue")
            ).filter { it.value > 0 }

            val dataSet = PieDataSet(entries, "").apply {
                colors = listOf(
                    AndroidColor.parseColor("#4CAF50"), // Green
                    AndroidColor.parseColor("#FF9800"), // Orange
                    AndroidColor.parseColor("#9E9E9E"), // Grey
                    AndroidColor.parseColor("#F44336")  // Red
                )
                sliceSpace = 3f
                selectionShift = 5f
                valueTextColor = AndroidColor.WHITE
                valueTextSize = 12f
                valueTypeface = Typeface.DEFAULT_BOLD
            }

            chart.centerText = "Total\n${summary.totalTasks}"
            chart.data = PieData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun WorkloadBarChart(workload: List<AssigneeWorkloadResponse>) {
    if (workload.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No workload data", color = Color.Gray)
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setDrawGridBackground(false)

                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                xAxis.textColor = AndroidColor.DKGRAY
                xAxis.setDrawAxisLine(false)

                axisLeft.setDrawGridLines(true)
                axisLeft.gridColor = AndroidColor.LTGRAY
                axisLeft.textColor = AndroidColor.GRAY
                axisLeft.axisMinimum = 0f

                axisRight.isEnabled = false
                legend.isEnabled = false
                animateY(1000)
            }
        },
        update = { chart ->
            val entries = workload.mapIndexed { index, item ->
                BarEntry(index.toFloat(), item.taskCount.toFloat())
            }

            val labels = workload.map { it.assigneeName?.take(10) ?: "User" }

            val dataSet = BarDataSet(entries, "Tasks").apply {
                color = AndroidColor.parseColor("#1A1C2FFF")
                valueTextSize = 11f
                valueTextColor = AndroidColor.BLACK
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.labelCount = labels.size.coerceAtMost(5)
            chart.data = BarData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun ProductivityLineChart(points: List<TimeSeriesPointResponse>) {
    if (points.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No productivity data available", color = Color.Gray)
        }
        return
    }

    val values = points.map { it.value.toFloat() }
    val labels = points.map { it.label }

    val modelProducer = remember { CartesianChartModelProducer.build() }

    LaunchedEffect(values) {
        modelProducer.tryRunTransaction {
            lineSeries {
                series(values)
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(
                label = rememberAxisLabelComponent(),
                valueFormatter = { value, _, _ ->
                    labels.getOrNull(value.toInt()) ?: ""
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    )
}
