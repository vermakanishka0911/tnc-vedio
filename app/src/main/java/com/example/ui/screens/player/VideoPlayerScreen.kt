package com.example.ui.screens.player

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import com.example.ui.components.DownloadIconVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.api.RetrofitClient
import com.example.data.model.Session
import com.example.data.repository.TncRepository
import com.example.ui.components.ErrorState
import com.example.ui.components.LoadingState
import com.example.ui.theme.MintBadge
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    sessionId: String,
    repository: TncRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPdf: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val completedLessons by repository.completedLessonsFlow.collectAsState()
    val isCompleted = completedLessons.contains(sessionId)

    val activeDownloads by repository.downloadManager.downloadProgressFlow.collectAsState()
    val isDownloading = activeDownloads.containsKey(sessionId)
    val downloadProgress = activeDownloads[sessionId] ?: 0f

    val downloadedLessons by repository.downloadManager.downloadedLessonsFlow.collectAsState()
    val isDownloaded = repository.downloadManager.isDownloaded(sessionId)
    val downloadedItem = downloadedLessons.find { it.sessionId == sessionId }

    fun loadSession() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            // First check if already downloaded offline: allows instant playback even with zero network!
            val offlineRecord = repository.prefs.getDownloadedLessons().find { it.sessionId == sessionId }
            if (offlineRecord != null) {
                val localFile = File(offlineRecord.localFilePath)
                if (localFile.exists() && localFile.length() > 0) {
                    session = Session(
                        rowId = offlineRecord.sessionId,
                        title = offlineRecord.title,
                        courseId = offlineRecord.courseId,
                        subjectId = offlineRecord.subjectId,
                        duration = offlineRecord.duration,
                        serialNo = offlineRecord.serialNo,
                        videoUrl = "file://${localFile.absolutePath}",
                        pdfUrl = offlineRecord.localPdfPath
                    )
                    isLoading = false
                    return@launch
                }
            }

            // Otherwise fetch from network
            val result = repository.getSession(sessionId)
            if (result.isSuccess) {
                val data = result.getOrNull()
                session = data
                isLoading = false
                data?.let {
                    repository.recordWatch(
                        sessionId = it.rowId,
                        title = it.title,
                        courseId = it.courseId,
                        subjectId = it.subjectId,
                        durationSec = 30L
                    )
                }
            } else {
                if (offlineRecord != null) {
                    isLoading = false
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load video lecture. Check your connection or open offline downloads."
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(sessionId) {
        loadSession()
    }

    // Heartbeat study timer (runs every 30 seconds while playing)
    LaunchedEffect(sessionId) {
        while (isActive) {
            delay(30_000)
            repository.sendStudyHeartbeat(sessionId, 30)
        }
    }

    // Cleanup webview on leave
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }

    Scaffold(
        modifier = modifier.testTag("video_player_screen"),
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = {
                        Text(
                            text = session?.title ?: "Video Lecture",
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
                    actions = {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Studying '${session?.title}' on TNC Nursing Classes! https://courses.tncnursing.site/courses/${session?.courseId ?: ""}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Lecture"))
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) Modifier.padding(0.dp).let { innerPadding } else innerPadding)
        ) {
            if (isLoading) {
                LoadingState(message = "Loading lecture stream & offline check...")
            } else if (errorMessage != null) {
                ErrorState(
                    message = errorMessage ?: "Error",
                    onRetry = { loadSession() }
                )
            } else if (session != null) {
                val currentSession = session!!
                val videoUrl = resolvePlaybackUrl(currentSession, isDownloaded, repository.downloadManager.getLocalFile(currentSession.rowId))

                if (isLandscape) {
                    // Full-screen video view in landscape orientation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .testTag("video_viewport_fullscreen")
                    ) {
                        if (videoUrl != null) {
                            HtmlVideoPlayer(
                                url = videoUrl,
                                autoPlay = true,
                                onWebViewCreated = { webViewRef = it }
                            )
                        }
                    }
                } else {
                    // Portrait orientation with responsive layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Video Viewport Box with 16:9 aspect ratio
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                                .testTag("video_viewport")
                        ) {
                            if (videoUrl != null) {
                                HtmlVideoPlayer(
                                    url = videoUrl,
                                    autoPlay = true,
                                    onWebViewCreated = { webViewRef = it }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No playable video source found",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Responsive details container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp)
                                .padding(16.dp)
                        ) {
                            // Lecture badge & completion status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Lecture #${currentSession.serialNo ?: "1"}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.toggleCompleted(currentSession.rowId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCompleted) MintBadge else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("mark_completed_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isCompleted) "Completed" else "Mark Complete")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentSession.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (!currentSession.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentSession.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Offline In-App Download Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDownloaded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isDownloaded) Icons.Default.CheckCircle else DownloadIconVector,
                                                contentDescription = null,
                                                tint = TealPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (isDownloaded) "Downloaded for Offline Use" else "Offline Access",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Stored privately inside TNC Nursing app (never in gallery)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (isDownloading) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Downloading lecture securely...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TealPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "${(downloadProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { downloadProgress },
                                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                                color = TealPrimary
                                            )
                                        }
                                    } else if (isDownloaded) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Size: ${downloadedItem?.fileSizeFormatted ?: "Saved"} • 100% Offline Ready",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TealPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Button(
                                                onClick = {
                                                    repository.downloadManager.deleteDownloadedLecture(currentSession.rowId)
                                                    Toast.makeText(context, "Offline download removed", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                                modifier = Modifier.testTag("delete_download_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Delete", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.downloadManager.downloadLecture(
                                                        session = currentSession,
                                                        courseName = currentSession.title,
                                                        onSuccess = {
                                                            Toast.makeText(context, "Download complete! Ready for offline study.", Toast.LENGTH_LONG).show()
                                                        },
                                                        onError = { err ->
                                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("download_lecture_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                                        ) {
                                            Icon(imageVector = DownloadIconVector, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Download for Offline Learning")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Attached Notes Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Study Notes & Curriculum",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (currentSession.hasPdf) {
                                        Button(
                                            onClick = { onNavigateToPdf(currentSession.rowId) },
                                            modifier = Modifier.fillMaxWidth().testTag("view_notes_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Open Attached E-Notes (PDF)")
                                        }
                                    } else {
                                        Text(
                                            text = "No separate PDF notes attached to this lecture.",
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
        }
    }
}

private fun resolvePlaybackUrl(session: Session, isDownloaded: Boolean, localFile: File): String? {
    if (isDownloaded && localFile.exists() && localFile.length() > 0) {
        return "file://${localFile.absolutePath}"
    }

    if (!session.firebaseId.isNullOrBlank()) {
        val encoded = URLEncoder.encode(session.firebaseId, StandardCharsets.UTF_8.toString())
        return "https://videoplay.tncnursing.in/videos/fs/index.html?$encoded"
    }

    val url = session.videoUrl ?: return null
    val youtubeId = extractYouTubeId(url)
    if (youtubeId != null) {
        return "https://www.youtube.com/embed/$youtubeId?autoplay=1&playsinline=1&enablejsapi=1"
    }

    return if (url.startsWith("/")) {
        "${RetrofitClient.BASE_URL.trimEnd('/')}$url"
    } else {
        url
    }
}

private fun extractYouTubeId(url: String): String? {
    val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
    val compiledPattern = java.util.regex.Pattern.compile(pattern)
    val matcher = compiledPattern.matcher(url)
    return if (matcher.find()) matcher.group() else null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlVideoPlayer(
    url: String,
    autoPlay: Boolean = true,
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    allowFileAccess = true
                    allowContentAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        if (autoPlay) {
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var v = document.querySelector('video');
                                    if (v) {
                                        v.play().catch(function(e) {
                                            console.log('Autoplay fallback', e);
                                        });
                                    }
                                })();
                                """.trimIndent(),
                                null
                            )
                        }
                    }
                }
                onWebViewCreated(this)

                if (url.startsWith("file://")) {
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>body { margin: 0; background-color: #000; display: flex; justify-content: center; align-items: center; height: 100vh; } video { width: 100%; height: 100%; object-fit: contain; }</style>
                        </head>
                        <body>
                        <video id="player" src="$url" autoplay controls playsinline></video>
                        <script>
                        window.onload = function() {
                            var p = document.getElementById('player');
                            if (p) p.play();
                        };
                        </script>
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL("file://", html, "text/html", "UTF-8", null)
                } else if (url.contains("youtube.com/embed")) {
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>body { margin: 0; background-color: #000; display: flex; justify-content: center; align-items: center; height: 100vh; } iframe { width: 100%; height: 100%; border: none; }</style>
                        </head>
                        <body>
                        <iframe id="ytplayer" src="$url" allowfullscreen allow="autoplay; encrypted-media"></iframe>
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                } else {
                    loadUrl(url)
                }
            }
        },
        update = { webView ->
            // Update webview if url changed
        },
        modifier = Modifier.fillMaxSize()
    )
}
