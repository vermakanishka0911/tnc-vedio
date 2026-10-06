package com.example.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Course
import com.example.data.model.HistoryItem
import com.example.data.model.Slider
import com.example.data.repository.TncRepository
import com.example.ui.components.CourseCard
import com.example.ui.components.ErrorState
import com.example.ui.components.LoadingState
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MintBadge
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.launch

private const val TEST_SERIES_URL = "https://test.tncnursing.site/tnc-tests"

@Composable
fun HomeScreen(
    repository: TncRepository,
    onNavigateToCourses: () -> Unit,
    onNavigateToCourseDetail: (String, String) -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToWatched: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var sliders by remember { mutableStateOf<List<Slider>>(emptyList()) }
    var recentHistory by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }

    val favorites by repository.favoriteCoursesFlow.collectAsState()
    val userName = remember { repository.prefs.getUserName().ifBlank { "Nursing Aspirant" } }
    val streakDays = remember { repository.prefs.getStreakDays() }
    val totalSeconds = remember { repository.prefs.getTotalStudySeconds() }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            repository.checkInstallStatus()
            val coursesResult = repository.getCourses()
            val slidersResult = repository.getSliders()
            recentHistory = repository.getWatchHistory().take(5)

            if (coursesResult.isSuccess) {
                courses = coursesResult.getOrDefault(emptyList())
                sliders = slidersResult.getOrDefault(emptyList())
                isLoading = false
            } else {
                errorMessage = coursesResult.exceptionOrNull()?.localizedMessage ?: "Failed to load courses"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (isLoading) {
        LoadingState(modifier = modifier, message = "Loading TNC Nursing Classes...")
        return
    }

    if (errorMessage != null) {
        ErrorState(
            modifier = modifier,
            message = errorMessage ?: "Error",
            onRetry = { loadData() }
        )
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
        // Welcome Header & Streak
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Namaste, $userName 👋",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "India's #1 Nursing Exam Prep",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        // Streak chip
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CoralAccent,
                            modifier = Modifier.testTag("streak_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$streakDays Days",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Study Time",
                            value = "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m",
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Completed",
                            value = "${repository.prefs.getCompletedLessons().size} Lectures",
                            icon = Icons.Default.Assignment,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Category Action Cards
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionItem(
                        title = "Courses",
                        icon = Icons.Default.MenuBook,
                        color = TealPrimary,
                        onClick = onNavigateToCourses,
                        modifier = Modifier.weight(1f).testTag("quick_action_courses")
                    )
                    QuickActionItem(
                        title = "Test Series",
                        icon = Icons.Default.OpenInNew,
                        color = CoralAccent,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TEST_SERIES_URL))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).testTag("quick_action_test_series")
                    )
                    QuickActionItem(
                        title = "Watched",
                        icon = Icons.Default.PlayCircle,
                        color = MintBadge,
                        onClick = onNavigateToWatched,
                        modifier = Modifier.weight(1f).testTag("quick_action_watched")
                    )
                    QuickActionItem(
                        title = "Ranks",
                        icon = Icons.Default.EmojiEvents,
                        color = GoldAccent,
                        onClick = onNavigateToLeaderboard,
                        modifier = Modifier.weight(1f).testTag("quick_action_leaderboard")
                    )
                }
            }
        }

        // Promotional Sliders
        if (sliders.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Highlights & Updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sliders) { slider ->
                            SliderBanner(slider = slider)
                        }
                    }
                }
            }
        }

        // Continue Learning (Recent Watched)
        if (recentHistory.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue Watching",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodySmall,
                            color = TealPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateToWatched() }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    recentHistory.firstOrNull()?.let { recent ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPlayer(recent.sessionId) }
                                .testTag("continue_watching_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TealPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recent.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Tap to resume lecture",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Featured Courses
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NORCET & Nursing Courses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View all (${courses.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TealPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToCourses() }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        items(courses.take(6)) { course ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                CourseCard(
                    course = course,
                    isFavorite = favorites.contains(course.rowId),
                    onFavoriteToggle = { repository.toggleFavorite(course.rowId) },
                    onClick = { onNavigateToCourseDetail(course.rowId, course.name) }
                )
            }
        }
    }
}
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SliderBanner(slider: Slider) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(130.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!slider.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = slider.imageUrl,
                    contentDescription = slider.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TealPrimary)
                        .padding(14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = slider.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (!slider.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = slider.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
