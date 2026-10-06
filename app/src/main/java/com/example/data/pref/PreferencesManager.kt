package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DownloadedLesson
import com.example.data.model.HistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tnc_nursing_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val ONE_WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000L

    init {
        checkWeeklyReset()
    }

    fun hasCompletedNamePrompt(): Boolean {
        return prefs.getBoolean(KEY_HAS_NAME_PROMPT, false)
    }

    fun setCompletedNamePrompt(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_NAME_PROMPT, completed).apply()
    }

    fun getInstallId(): String {
        var id = prefs.getString(KEY_INSTALL_ID, null)
        if (id.isNullOrBlank()) {
            val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(10)
            id = "tnc_${System.currentTimeMillis()}_$randomSuffix"
            prefs.edit().putString(KEY_INSTALL_ID, id).apply()
        }
        return id
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    fun setUserName(name: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, name.trim())
            .putBoolean(KEY_HAS_NAME_PROMPT, true)
            .apply()
    }

    fun getFavoriteCourses(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(courseRowId: String): Boolean {
        val current = getFavoriteCourses().toMutableSet()
        val isFav: Boolean
        if (current.contains(courseRowId)) {
            current.remove(courseRowId)
            isFav = false
        } else {
            current.add(courseRowId)
            isFav = true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return isFav
    }

    fun isFavorite(courseRowId: String): Boolean {
        return getFavoriteCourses().contains(courseRowId)
    }

    fun getCompletedLessons(): Set<String> {
        return prefs.getStringSet(KEY_COMPLETED, emptySet()) ?: emptySet()
    }

    fun toggleCompletedLesson(sessionRowId: String): Boolean {
        val current = getCompletedLessons().toMutableSet()
        val isCompleted: Boolean
        if (current.contains(sessionRowId)) {
            current.remove(sessionRowId)
            isCompleted = false
        } else {
            current.add(sessionRowId)
            isCompleted = true
        }
        prefs.edit().putStringSet(KEY_COMPLETED, current).apply()
        return isCompleted
    }

    fun isLessonCompleted(sessionRowId: String): Boolean {
        return getCompletedLessons().contains(sessionRowId)
    }

    fun addWatchHistory(sessionId: String, title: String, courseId: String?, subjectId: String?, durationSec: Long) {
        val history = getWatchHistory().toMutableList()
        history.removeAll { it.sessionId == sessionId }
        history.add(0, HistoryItem(
            sessionId = sessionId,
            title = title,
            courseId = courseId,
            subjectId = subjectId,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSec
        ))
        val trimmed = history.take(50)
        prefs.edit().putString(KEY_HISTORY, gson.toJson(trimmed)).apply()

        addStudyTime(durationSec)
    }

    fun getWatchHistory(): List<HistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearWatchHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    // Weekly Auto-Reset Logic
    fun checkWeeklyReset() {
        val now = System.currentTimeMillis()
        var weekStart = prefs.getLong(KEY_WEEK_START_TIMESTAMP, 0L)
        if (weekStart == 0L || (now - weekStart) >= ONE_WEEK_MILLIS) {
            // New week started: Auto-reset weekly leaderboard study stats
            prefs.edit()
                .putLong(KEY_WEEK_START_TIMESTAMP, now)
                .putLong(KEY_WEEKLY_STUDY_SECONDS, 0L)
                .putInt(KEY_WEEK_CYCLE_COUNT, prefs.getInt(KEY_WEEK_CYCLE_COUNT, 0) + 1)
                .apply()
        }
    }

    fun addStudyTime(seconds: Long) {
        checkWeeklyReset()
        val currentTotal = prefs.getLong(KEY_STUDY_SECONDS, 0L)
        val currentWeekly = prefs.getLong(KEY_WEEKLY_STUDY_SECONDS, 0L)
        prefs.edit()
            .putLong(KEY_STUDY_SECONDS, currentTotal + seconds)
            .putLong(KEY_WEEKLY_STUDY_SECONDS, currentWeekly + seconds)
            .apply()
        updateStreak()
    }

    fun getTotalStudySeconds(): Long {
        return prefs.getLong(KEY_STUDY_SECONDS, 0L)
    }

    fun getWeeklyStudySeconds(): Long {
        checkWeeklyReset()
        return prefs.getLong(KEY_WEEKLY_STUDY_SECONDS, 0L)
    }

    fun getWeeklyResetRemainingTime(): String {
        checkWeeklyReset()
        val now = System.currentTimeMillis()
        val weekStart = prefs.getLong(KEY_WEEK_START_TIMESTAMP, now)
        val nextReset = weekStart + ONE_WEEK_MILLIS
        val diff = maxOf(0L, nextReset - now)

        val days = diff / (24 * 60 * 60 * 1000L)
        val hours = (diff % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)
        return if (days > 0) "$days days, $hours hrs" else "$hours hrs"
    }

    private fun updateStreak() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastDate = prefs.getString(KEY_LAST_ACTIVE_DATE, "")
        if (lastDate != today) {
            val streak = prefs.getInt(KEY_STREAK, 0)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 86400000L))
            val newStreak = if (lastDate == yesterday) streak + 1 else 1
            prefs.edit()
                .putString(KEY_LAST_ACTIVE_DATE, today)
                .putInt(KEY_STREAK, newStreak)
                .apply()
        }
    }

    fun getStreakDays(): Int {
        return prefs.getInt(KEY_STREAK, 1)
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "system") ?: "system"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun isBlocked(): Boolean {
        return prefs.getBoolean(KEY_IS_BLOCKED, false)
    }

    fun setBlocked(blocked: Boolean, reason: String? = null) {
        prefs.edit()
            .putBoolean(KEY_IS_BLOCKED, blocked)
            .putString(KEY_BLOCK_REASON, reason ?: "")
            .apply()
    }

    fun getBlockReason(): String {
        return prefs.getString(KEY_BLOCK_REASON, "Access paused by administrator") ?: "Access paused"
    }

    // Admin mode
    fun isAdmin(): Boolean {
        return prefs.getBoolean(KEY_IS_ADMIN, false)
    }

    fun setAdmin(isAdmin: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ADMIN, isAdmin).apply()
    }

    // Offline Downloaded Lessons (Private App Storage - Never in Gallery)
    fun getDownloadedLessons(): List<DownloadedLesson> {
        val json = prefs.getString(KEY_DOWNLOADED_LESSONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DownloadedLesson>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDownloadedLesson(item: DownloadedLesson) {
        val list = getDownloadedLessons().toMutableList()
        list.removeAll { it.sessionId == item.sessionId }
        list.add(0, item)
        prefs.edit().putString(KEY_DOWNLOADED_LESSONS, gson.toJson(list)).apply()
    }

    fun removeDownloadedLesson(sessionId: String): Boolean {
        val list = getDownloadedLessons().toMutableList()
        val removed = list.removeAll { it.sessionId == sessionId }
        if (removed) {
            prefs.edit().putString(KEY_DOWNLOADED_LESSONS, gson.toJson(list)).apply()
        }
        return removed
    }

    fun isLessonDownloaded(sessionId: String): Boolean {
        return getDownloadedLessons().any { it.sessionId == sessionId }
    }

    fun getLocalFilePath(sessionId: String): String? {
        return getDownloadedLessons().find { it.sessionId == sessionId }?.localFilePath
    }

    companion object {
        private const val KEY_INSTALL_ID = "tnc_install_id"
        private const val KEY_USER_NAME = "tnc_user_name"
        private const val KEY_HAS_NAME_PROMPT = "tnc_has_name_prompt"
        private const val KEY_FAVORITES = "tnc_favorites"
        private const val KEY_COMPLETED = "tnc_completed_lessons"
        private const val KEY_HISTORY = "tnc_watch_history"
        private const val KEY_DOWNLOADED_LESSONS = "tnc_downloaded_lessons"
        private const val KEY_STUDY_SECONDS = "tnc_study_seconds"
        private const val KEY_WEEKLY_STUDY_SECONDS = "tnc_weekly_study_seconds"
        private const val KEY_WEEK_START_TIMESTAMP = "tnc_week_start_timestamp"
        private const val KEY_WEEK_CYCLE_COUNT = "tnc_week_cycle_count"
        private const val KEY_STREAK = "tnc_streak_days"
        private const val KEY_LAST_ACTIVE_DATE = "tnc_last_active_date"
        private const val KEY_THEME_MODE = "tnc_theme_mode"
        private const val KEY_IS_BLOCKED = "tnc_is_blocked"
        private const val KEY_BLOCK_REASON = "tnc_block_reason"
        private const val KEY_IS_ADMIN = "tnc_is_admin"
    }
}
