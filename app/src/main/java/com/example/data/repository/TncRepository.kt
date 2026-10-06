package com.example.data.repository

import android.content.Context
import com.example.data.api.RetrofitClient
import com.example.data.download.OfflineDownloadManager
import com.example.data.model.Course
import com.example.data.model.HeartbeatRequest
import com.example.data.model.HistoryItem
import com.example.data.model.MobileOpenRequest
import com.example.data.model.ProgressRequest
import com.example.data.model.Session
import com.example.data.model.Slider
import com.example.data.model.StudyLeaderboardRow
import com.example.data.model.Subject
import com.example.data.pref.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class TncRepository(private val context: Context) {
    private val api = RetrofitClient.getService(context)
    val prefs = PreferencesManager(context)
    val downloadManager = OfflineDownloadManager(context)

    private val _completedLessonsFlow = MutableStateFlow(prefs.getCompletedLessons())
    val completedLessonsFlow: StateFlow<Set<String>> = _completedLessonsFlow

    private val _favoriteCoursesFlow = MutableStateFlow(prefs.getFavoriteCourses())
    val favoriteCoursesFlow: StateFlow<Set<String>> = _favoriteCoursesFlow

    suspend fun checkInstallStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.mobileOpen(
                MobileOpenRequest(
                    installId = prefs.getInstallId(),
                    platform = "android",
                    appVersion = "2.0.0"
                )
            )
            if (response.blocked) {
                prefs.setBlocked(true, response.reason)
                true
            } else {
                prefs.setBlocked(false)
                false
            }
        } catch (_: Exception) {
            prefs.isBlocked()
        }
    }

    suspend fun getCourses(): Result<List<Course>> = withContext(Dispatchers.IO) {
        try {
            val courses = api.getCourses()
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubjects(courseId: String): Result<List<Subject>> = withContext(Dispatchers.IO) {
        try {
            val subjects = api.getSubjects(courseId)
            Result.success(subjects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessions(
        courseId: String? = null,
        subjectId: String? = null,
        type: String? = null,
        search: String? = null,
        sort: String? = null
    ): Result<List<Session>> = withContext(Dispatchers.IO) {
        try {
            val sessions = api.getSessions(
                courseId = courseId,
                subjectId = subjectId,
                type = type,
                search = search,
                sort = sort
            )
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSession(sessionId: String): Result<Session> = withContext(Dispatchers.IO) {
        try {
            val session = api.getSession(sessionId)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotes(courseId: String? = null): Result<List<Session>> = withContext(Dispatchers.IO) {
        try {
            val notes = api.getNotes(courseId)
            Result.success(notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSliders(): Result<List<Slider>> = withContext(Dispatchers.IO) {
        try {
            val sliders = api.getSliders()
            Result.success(sliders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeaderboard(limit: Int = 30): Result<List<StudyLeaderboardRow>> = withContext(Dispatchers.IO) {
        try {
            val leaderboard = api.getLeaderboard(limit)
            Result.success(leaderboard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendStudyHeartbeat(sessionId: String, seconds: Int = 30) = withContext(Dispatchers.IO) {
        try {
            api.sendHeartbeat(
                HeartbeatRequest(
                    visitorId = prefs.getInstallId(),
                    visitorName = prefs.getUserName(),
                    sessionId = sessionId,
                    seconds = seconds
                )
            )
            prefs.addStudyTime(seconds.toLong())
        } catch (_: Exception) {
            // Heartbeat failure shouldn't disrupt learning
        }
    }

    suspend fun toggleCompleted(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val newState = prefs.toggleCompletedLesson(sessionId)
        _completedLessonsFlow.value = prefs.getCompletedLessons()
        try {
            api.recordProgress(
                ProgressRequest(
                    installId = prefs.getInstallId(),
                    sessionId = sessionId,
                    completed = newState
                )
            )
        } catch (_: Exception) {
            // Keep local state
        }
        newState
    }

    fun toggleFavorite(courseRowId: String): Boolean {
        val newState = prefs.toggleFavorite(courseRowId)
        _favoriteCoursesFlow.value = prefs.getFavoriteCourses()
        return newState
    }

    fun recordWatch(sessionId: String, title: String, courseId: String?, subjectId: String?, durationSec: Long) {
        prefs.addWatchHistory(sessionId, title, courseId, subjectId, durationSec)
    }

    fun getWatchHistory(): List<HistoryItem> {
        return prefs.getWatchHistory()
    }

    fun clearHistory() {
        prefs.clearWatchHistory()
    }
}
