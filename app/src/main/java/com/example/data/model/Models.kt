package com.example.data.model

import com.google.gson.annotations.SerializedName

data class Course(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("rowId") val rowId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("serialNo") val serialNo: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class Subject(
    @SerializedName("rowId") val rowId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("serialNo") val serialNo: String? = null,
    @SerializedName("videoCount") val videoCount: Int = 0,
    @SerializedName("pdfCount") val pdfCount: Int = 0,
    @SerializedName("totalCount") val totalCount: Int = 0
)

data class Session(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("rowId") val rowId: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("videoUrl") val videoUrl: String? = null,
    @SerializedName("pdfUrl") val pdfUrl: String? = null,
    @SerializedName("firebaseId") val firebaseId: String? = null,
    @SerializedName("contentType") val contentType: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("courseId") val courseId: String? = null,
    @SerializedName("subjectId") val subjectId: String? = null,
    @SerializedName("isPaid") val isPaid: Boolean = false,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("serialNo") val serialNo: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    val serialNumberInt: Int
        get() = serialNo?.toIntOrNull() ?: 9999

    val hasVideo: Boolean
        get() = !firebaseId.isNullOrBlank() || !videoUrl.isNullOrBlank()

    val hasPdf: Boolean
        get() = !pdfUrl.isNullOrBlank()
}

data class Slider(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("rowId") val rowId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null
)

data class StudyLeaderboardRow(
    @SerializedName("telegramId") val telegramId: Any? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("visitorName") val visitorName: String? = null,
    @SerializedName("seconds") val seconds: Long = 0,
    @SerializedName("sessions") val sessions: Int = 0
) {
    val displayName: String
        get() = when {
            !firstName.isNullOrBlank() -> firstName
            !visitorName.isNullOrBlank() -> visitorName
            else -> "Nursing Scholar"
        }

    val formattedTime: String
        get() {
            val mins = seconds / 60
            val hours = mins / 60
            val remainingMins = mins % 60
            return if (hours > 0) "${hours}h ${remainingMins}m" else "${mins}m"
        }
}

data class MobileOpenResponse(
    @SerializedName("blocked") val blocked: Boolean = false,
    @SerializedName("reason") val reason: String? = null
)

data class HeartbeatRequest(
    @SerializedName("visitorId") val visitorId: String,
    @SerializedName("visitorName") val visitorName: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("seconds") val seconds: Int
)

data class ProgressRequest(
    @SerializedName("installId") val installId: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("completed") val completed: Boolean
)

data class MobileOpenRequest(
    @SerializedName("installId") val installId: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("appVersion") val appVersion: String = "2.0.0"
)

data class HistoryItem(
    val sessionId: String,
    val title: String,
    val courseId: String?,
    val subjectId: String?,
    val timestamp: Long,
    val durationSeconds: Long
)

data class DownloadedLesson(
    val sessionId: String,
    val title: String,
    val courseId: String?,
    val subjectId: String?,
    val localFilePath: String,
    val fileSizeFormatted: String,
    val downloadedAt: Long,
    val duration: String?,
    val serialNo: String?,
    val hasPdf: Boolean = false,
    val localPdfPath: String? = null
)
