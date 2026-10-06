package com.example.ui.screens.courses

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Session
import com.example.data.repository.TncRepository
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorState
import com.example.ui.components.LessonCard
import com.example.ui.components.LoadingState
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    courseId: String,
    subjectId: String,
    subjectName: String,
    repository: TncRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToPdf: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("all") } // "all", "video", "pdf"
    var sortOrder by remember { mutableStateOf("serial") } // "serial", "newest", "oldest"

    val completedLessons by repository.completedLessonsFlow.collectAsState()
    val downloadedLessons by repository.downloadManager.downloadedLessonsFlow.collectAsState()
    val activeDownloads by repository.downloadManager.downloadProgressFlow.collectAsState()

    fun loadSessions() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = repository.getSessions(courseId = courseId, subjectId = subjectId)
            if (result.isSuccess) {
                sessions = result.getOrDefault(emptyList())
                isLoading = false
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load lessons"
                isLoading = false
            }
        }
    }

    LaunchedEffect(courseId, subjectId) {
        loadSessions()
    }

    val filteredAndSortedSessions = sessions.filter { session ->
        val matchesQuery = searchQuery.isBlank() ||
                session.title.contains(searchQuery, ignoreCase = true) ||
                (session.description?.contains(searchQuery, ignoreCase = true) == true)
        val matchesType = when (filterType) {
            "video" -> session.hasVideo
            "pdf" -> session.hasPdf
            else -> true
        }
        matchesQuery && matchesType
    }.let { list ->
        when (sortOrder) {
            "serial" -> list.sortedWith(compareBy({ it.serialNumberInt }, { it.rowId }))
            "newest" -> list.reversed()
            "oldest" -> list
            else -> list
        }
    }

    Scaffold(
        modifier = modifier.testTag("subject_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = subjectName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                // Search and Filters bar
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_search_input"),
                        placeholder = { Text("Search lessons in $subjectName...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
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

                    // Filter & Sort chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = filterType == "all",
                                onClick = { filterType = "all" },
                                label = { Text("All (${sessions.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = filterType == "video",
                                onClick = { filterType = "video" },
                                label = { Text("Videos") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = filterType == "pdf",
                                onClick = { filterType = "pdf" },
                                label = { Text("E-Notes") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        // Sort toggle
                        FilterChip(
                            selected = sortOrder != "serial",
                            onClick = {
                                sortOrder = when (sortOrder) {
                                    "serial" -> "newest"
                                    "newest" -> "oldest"
                                    else -> "serial"
                                }
                            },
                            label = {
                                Text(
                                    when (sortOrder) {
                                        "newest" -> "Newest"
                                        "oldest" -> "Oldest"
                                        else -> "Order #"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Sort, contentDescription = null)
                            }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        LoadingState(message = "Loading Lessons...")
                    } else if (errorMessage != null) {
                        ErrorState(
                            message = errorMessage ?: "Error",
                            onRetry = { loadSessions() }
                        )
                    } else if (filteredAndSortedSessions.isEmpty()) {
                        EmptyState(
                            title = "No lessons found",
                            description = "Try changing your search or filter options."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredAndSortedSessions, key = { it.rowId }) { session ->
                                val isDownloaded = downloadedLessons.any { it.sessionId == session.rowId }
                                val isDownloading = activeDownloads.containsKey(session.rowId)

                                LessonCard(
                                    session = session,
                                    isCompleted = completedLessons.contains(session.rowId),
                                    isDownloaded = isDownloaded,
                                    isDownloading = isDownloading,
                                    onCompleteToggle = {
                                        coroutineScope.launch {
                                            repository.toggleCompleted(session.rowId)
                                        }
                                    },
                                    onDownloadClick = {
                                        coroutineScope.launch {
                                            if (isDownloaded) {
                                                repository.downloadManager.deleteDownloadedLecture(session.rowId)
                                                Toast.makeText(context, "Offline download removed", Toast.LENGTH_SHORT).show()
                                            } else {
                                                repository.downloadManager.downloadLecture(
                                                    session = session,
                                                    courseName = subjectName,
                                                    onSuccess = {
                                                        Toast.makeText(context, "Saved offline in app!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    onPlayClick = {
                                        if (session.hasVideo) {
                                            onNavigateToPlayer(session.rowId)
                                        } else if (session.hasPdf) {
                                            onNavigateToPdf(session.rowId)
                                        }
                                    },
                                    onPdfClick = if (session.hasPdf) {
                                        { onNavigateToPdf(session.rowId) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
