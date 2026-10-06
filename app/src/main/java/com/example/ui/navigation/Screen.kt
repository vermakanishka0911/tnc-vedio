package com.example.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Courses : Screen("courses")
    object Watched : Screen("watched")
    object Leaderboard : Screen("leaderboard")
    object Profile : Screen("profile")

    object CourseDetail : Screen("course_detail/{courseId}/{courseName}") {
        fun createRoute(courseId: String, courseName: String): String {
            val encodedName = URLEncoder.encode(courseName, StandardCharsets.UTF_8.toString())
            return "course_detail/$courseId/$encodedName"
        }
    }

    object SubjectDetail : Screen("subject_detail/{courseId}/{subjectId}/{subjectName}") {
        fun createRoute(courseId: String, subjectId: String, subjectName: String): String {
            val encodedName = URLEncoder.encode(subjectName, StandardCharsets.UTF_8.toString())
            return "subject_detail/$courseId/$subjectId/$encodedName"
        }
    }

    object VideoPlayer : Screen("player/{sessionId}") {
        fun createRoute(sessionId: String): String = "player/$sessionId"
    }

    object PdfViewer : Screen("pdf/{sessionId}") {
        fun createRoute(sessionId: String): String = "pdf/$sessionId"
    }

    object Admin : Screen("admin")
}
