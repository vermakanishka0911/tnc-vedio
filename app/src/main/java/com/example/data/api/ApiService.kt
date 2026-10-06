package com.example.data.api

import com.example.data.model.Course
import com.example.data.model.HeartbeatRequest
import com.example.data.model.MobileOpenRequest
import com.example.data.model.MobileOpenResponse
import com.example.data.model.ProgressRequest
import com.example.data.model.Session
import com.example.data.model.Slider
import com.example.data.model.StudyLeaderboardRow
import com.example.data.model.Subject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/courses")
    suspend fun getCourses(): List<Course>

    @GET("api/subjects")
    suspend fun getSubjects(
        @Query("courseId") courseId: String
    ): List<Subject>

    @GET("api/sessions")
    suspend fun getSessions(
        @Query("courseId") courseId: String? = null,
        @Query("subjectId") subjectId: String? = null,
        @Query("type") type: String? = null,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int? = null
    ): List<Session>

    @GET("api/sessions/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String
    ): Session

    @GET("api/notes")
    suspend fun getNotes(
        @Query("courseId") courseId: String? = null
    ): List<Session>

    @GET("api/sliders")
    suspend fun getSliders(): List<Slider>

    @GET("api/bot/study/leaderboard")
    suspend fun getLeaderboard(
        @Query("limit") limit: Int = 30
    ): List<StudyLeaderboardRow>

    @POST("api/bot/study/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: HeartbeatRequest
    ): Response<ResponseBody>

    @POST("api/mobile/open")
    suspend fun mobileOpen(
        @Body request: MobileOpenRequest
    ): MobileOpenResponse

    @POST("api/mobile/progress")
    suspend fun recordProgress(
        @Body request: ProgressRequest
    ): Response<ResponseBody>
}
