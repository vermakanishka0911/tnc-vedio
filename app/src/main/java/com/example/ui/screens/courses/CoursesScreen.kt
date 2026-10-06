package com.example.ui.screens.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Course
import com.example.data.repository.TncRepository
import com.example.ui.components.CourseCard
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorState
import com.example.ui.components.LoadingState
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.launch

@Composable
fun CoursesScreen(
    repository: TncRepository,
    onNavigateToCourseDetail: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("all") } // "all" or "favorites"

    val favorites by repository.favoriteCoursesFlow.collectAsState()

    fun loadCourses() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = repository.getCourses()
            if (result.isSuccess) {
                courses = result.getOrDefault(emptyList())
                isLoading = false
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load courses"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCourses()
    }

    if (isLoading) {
        LoadingState(modifier = modifier, message = "Loading Courses...")
        return
    }

    if (errorMessage != null) {
        ErrorState(
            modifier = modifier,
            message = errorMessage ?: "Error",
            onRetry = { loadCourses() }
        )
        return
    }

    val filteredCourses = courses.filter { course ->
        val matchesQuery = searchQuery.isBlank() ||
                course.name.contains(searchQuery, ignoreCase = true) ||
                (course.description?.contains(searchQuery, ignoreCase = true) == true)
        val matchesTab = if (selectedTab == "favorites") favorites.contains(course.rowId) else true
        matchesQuery && matchesTab
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("courses_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
        // Search & Filter header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Explore Courses",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("course_search_input"),
                placeholder = { Text("Search by exam or topic...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("clear_search_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == "all",
                    onClick = { selectedTab = "all" },
                    label = { Text("All Courses (${courses.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_all_courses")
                )
                FilterChip(
                    selected = selectedTab == "favorites",
                    onClick = { selectedTab = "favorites" },
                    label = { Text("Favorites (${favorites.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_favorite_courses")
                )
            }
        }

        if (filteredCourses.isEmpty()) {
            EmptyState(
                title = if (selectedTab == "favorites") "No favorite courses yet" else "No matching courses found",
                description = if (selectedTab == "favorites") "Bookmark any course to access it quickly here." else "Try adjusting your search query."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredCourses, key = { it.rowId }) { course ->
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

