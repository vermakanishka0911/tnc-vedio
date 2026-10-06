package com.example.ui.screens.pdf

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.api.RetrofitClient
import com.example.data.model.Session
import com.example.data.repository.TncRepository
import com.example.ui.components.ErrorState
import com.example.ui.components.LoadingState
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    sessionId: String,
    repository: TncRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }

    fun loadSession() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = repository.getSession(sessionId)
            if (result.isSuccess) {
                session = result.getOrNull()
                isLoading = false
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to load notes"
                isLoading = false
            }
        }
    }

    LaunchedEffect(sessionId) {
        loadSession()
    }

    val rawPdfUrl = session?.pdfUrl?.let { url ->
        if (url.startsWith("/")) {
            "${RetrofitClient.BASE_URL.trimEnd('/')}$url"
        } else {
            url
        }
    }

    val googleDocsPdfUrl = rawPdfUrl?.let {
        val encoded = URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
        "https://docs.google.com/viewer?embedded=true&url=$encoded"
    }

    Scaffold(
        modifier = modifier.testTag("pdf_viewer_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = session?.title ?: "E-Notes",
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
                    if (rawPdfUrl != null) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawPdfUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.testTag("open_external_pdf_button")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                LoadingState(message = "Loading E-Notes PDF...")
            } else if (errorMessage != null) {
                ErrorState(
                    message = errorMessage ?: "Error",
                    onRetry = { loadSession() }
                )
            } else if (googleDocsPdfUrl != null) {
                PdfWebView(url = googleDocsPdfUrl)
            } else {
                ErrorState(
                    message = "No PDF file attached to this lecture.",
                    onRetry = onNavigateBack
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PdfWebView(url: String) {
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
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
