package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.TncRepository
import com.example.ui.screens.courses.CourseDetailScreen
import com.example.ui.screens.courses.CoursesScreen
import com.example.ui.screens.courses.SubjectDetailScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.pdf.PdfViewerScreen
import com.example.ui.screens.player.VideoPlayerScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.watched.WatchedScreen
import com.example.ui.theme.TealPrimary
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class NavTabItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AppNavigation(
    repository: TncRepository,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = listOf(
        NavTabItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
        NavTabItem(Screen.Courses.route, "Courses", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_tab_courses"),
        NavTabItem(Screen.Watched.route, "Watched", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_tab_watched"),
        NavTabItem(Screen.Leaderboard.route, "Ranks", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_leaderboard"),
        NavTabItem(Screen.Profile.route, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
    )

    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TealPrimary.copy(alpha = 0.15f),
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    repository = repository,
                    onNavigateToCourses = {
                        navController.navigate(Screen.Courses.route)
                    },
                    onNavigateToCourseDetail = { courseId, courseName ->
                        navController.navigate(Screen.CourseDetail.createRoute(courseId, courseName))
                    },
                    onNavigateToPlayer = { sessionId ->
                        navController.navigate(Screen.VideoPlayer.createRoute(sessionId))
                    },
                    onNavigateToWatched = {
                        navController.navigate(Screen.Watched.route)
                    },
                    onNavigateToLeaderboard = {
                        navController.navigate(Screen.Leaderboard.route)
                    }
                )
            }

            composable(Screen.Courses.route) {
                CoursesScreen(
                    repository = repository,
                    onNavigateToCourseDetail = { courseId, courseName ->
                        navController.navigate(Screen.CourseDetail.createRoute(courseId, courseName))
                    }
                )
            }

            composable(
                route = Screen.CourseDetail.route,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("courseName") { type = NavType.StringType }
                )
            ) { backStack ->
                val courseId = backStack.arguments?.getString("courseId") ?: ""
                val encodedName = backStack.arguments?.getString("courseName") ?: "Course"
                val courseName = try {
                    URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
                } catch (_: Exception) {
                    encodedName
                }

                CourseDetailScreen(
                    courseId = courseId,
                    courseName = courseName,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubjectDetail = { cId, sId, sName ->
                        navController.navigate(Screen.SubjectDetail.createRoute(cId, sId, sName))
                    }
                )
            }

            composable(
                route = Screen.SubjectDetail.route,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("subjectId") { type = NavType.StringType },
                    navArgument("subjectName") { type = NavType.StringType }
                )
            ) { backStack ->
                val courseId = backStack.arguments?.getString("courseId") ?: ""
                val subjectId = backStack.arguments?.getString("subjectId") ?: ""
                val encodedName = backStack.arguments?.getString("subjectName") ?: "Subject"
                val subjectName = try {
                    URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
                } catch (_: Exception) {
                    encodedName
                }

                SubjectDetailScreen(
                    courseId = courseId,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { sId ->
                        navController.navigate(Screen.VideoPlayer.createRoute(sId))
                    },
                    onNavigateToPdf = { sId ->
                        navController.navigate(Screen.PdfViewer.createRoute(sId))
                    }
                )
            }

            composable(
                route = Screen.VideoPlayer.route,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType }
                )
            ) { backStack ->
                val sessionId = backStack.arguments?.getString("sessionId") ?: ""
                VideoPlayerScreen(
                    sessionId = sessionId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPdf = { sId ->
                        navController.navigate(Screen.PdfViewer.createRoute(sId))
                    }
                )
            }

            composable(
                route = Screen.PdfViewer.route,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType }
                )
            ) { backStack ->
                val sessionId = backStack.arguments?.getString("sessionId") ?: ""
                PdfViewerScreen(
                    sessionId = sessionId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Watched.route) {
                WatchedScreen(
                    repository = repository,
                    onNavigateToPlayer = { sessionId ->
                        navController.navigate(Screen.VideoPlayer.createRoute(sessionId))
                    }
                )
            }

            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(repository = repository)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    repository = repository,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onNavigateToAdmin = {
                        navController.navigate(Screen.Admin.route)
                    }
                )
            }

            composable(Screen.Admin.route) {
                com.example.ui.screens.admin.AdminDashboardScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
