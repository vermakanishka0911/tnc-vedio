package com.example.data.download

import android.content.Context
import com.example.data.api.RetrofitClient
import com.example.data.model.DownloadedLesson
import com.example.data.model.Session
import com.example.data.pref.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class OfflineDownloadManager(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // Store downloads in internal sandbox directory: strictly isolated from Gallery
    private val offlineDir: File
        get() {
            val dir = File(context.filesDir, "offline_lectures")
            if (!dir.exists()) dir.mkdirs()
            // Add a .nomedia file as extra assurance that Android Gallery never indexes this folder
            val noMedia = File(dir, ".nomedia")
            if (!noMedia.exists()) noMedia.createNewFile()
            return dir
        }

    // Active downloads progress: sessionId -> progress (0.0 to 1.0)
    private val _downloadProgressFlow = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressFlow: StateFlow<Map<String, Float>> = _downloadProgressFlow

    private val _downloadedLessonsFlow = MutableStateFlow(prefs.getDownloadedLessons())
    val downloadedLessonsFlow: StateFlow<List<DownloadedLesson>> = _downloadedLessonsFlow

    fun isDownloaded(sessionId: String): Boolean {
        val file = getLocalFile(sessionId)
        return file.exists() && file.length() > 0 && prefs.isLessonDownloaded(sessionId)
    }

    fun getLocalFile(sessionId: String): File {
        return File(offlineDir, "lecture_$sessionId.mp4")
    }

    fun getDownloadedList(): List<DownloadedLesson> {
        return prefs.getDownloadedLessons().filter {
            val f = File(it.localFilePath)
            f.exists() && f.length() > 0
        }
    }

    suspend fun downloadLecture(
        session: Session,
        courseName: String = "Nursing Lecture",
        onProgress: (Float) -> Unit = {},
        onSuccess: (DownloadedLesson) -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val downloadUrl = resolveMediaDownloadUrl(session)
        if (downloadUrl == null) {
            withContext(Dispatchers.Main) {
                onError("No downloadable video stream URL found for this lecture.")
            }
            return@withContext
        }

        val targetFile = getLocalFile(session.rowId)

        try {
            updateProgress(session.rowId, 0.01f)

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("x-tnc-install-id", prefs.getInstallId())
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                updateProgress(session.rowId, null)
                withContext(Dispatchers.Main) {
                    onError("Download failed (Server status: ${response.code})")
                }
                return@withContext
            }

            val body = response.body ?: run {
                updateProgress(session.rowId, null)
                withContext(Dispatchers.Main) { onError("Empty video response from server") }
                return@withContext
            }

            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                    updateProgress(session.rowId, progress)
                    withContext(Dispatchers.Main) { onProgress(progress) }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            updateProgress(session.rowId, null)

            val formattedSize = formatFileSize(targetFile.length())
            val downloadedLesson = DownloadedLesson(
                sessionId = session.rowId,
                title = session.title,
                courseId = session.courseId,
                subjectId = session.subjectId,
                localFilePath = targetFile.absolutePath,
                fileSizeFormatted = formattedSize,
                downloadedAt = System.currentTimeMillis(),
                duration = session.duration,
                serialNo = session.serialNo,
                hasPdf = session.hasPdf
            )

            prefs.saveDownloadedLesson(downloadedLesson)
            _downloadedLessonsFlow.value = prefs.getDownloadedLessons()

            withContext(Dispatchers.Main) {
                onSuccess(downloadedLesson)
            }
        } catch (e: Exception) {
            updateProgress(session.rowId, null)
            if (targetFile.exists()) targetFile.delete()
            withContext(Dispatchers.Main) {
                onError("Download error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun deleteDownloadedLecture(sessionId: String): Boolean {
        val file = getLocalFile(sessionId)
        if (file.exists()) {
            file.delete()
        }
        val removed = prefs.removeDownloadedLesson(sessionId)
        _downloadedLessonsFlow.value = prefs.getDownloadedLessons()
        return removed
    }

    private fun updateProgress(sessionId: String, progress: Float?) {
        val current = _downloadProgressFlow.value.toMutableMap()
        if (progress == null) {
            current.remove(sessionId)
        } else {
            current[sessionId] = progress
        }
        _downloadProgressFlow.value = current
    }

    private fun resolveMediaDownloadUrl(session: Session): String? {
        if (!session.firebaseId.isNullOrBlank()) {
            val encodedId = URLEncoder.encode(session.firebaseId, StandardCharsets.UTF_8.toString())
            return "${RetrofitClient.BASE_URL.trimEnd('/')}/api/firebase-stream/$encodedId"
        }
        val url = session.videoUrl ?: return null
        return if (url.startsWith("/")) {
            "${RetrofitClient.BASE_URL.trimEnd('/')}$url"
        } else {
            url
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return if (mb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes.toDouble() / 1024
            String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }
}
