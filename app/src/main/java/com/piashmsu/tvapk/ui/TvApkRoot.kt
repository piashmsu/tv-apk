package com.piashmsu.tvapk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piashmsu.tvapk.data.Channel
import com.piashmsu.tvapk.data.Movie
import com.piashmsu.tvapk.data.PlaybackTarget
import com.piashmsu.tvapk.data.PlaybackTargetHolder
import com.piashmsu.tvapk.ui.screens.HomeScreen
import com.piashmsu.tvapk.ui.screens.LiveTvScreen
import com.piashmsu.tvapk.ui.screens.MoviesScreen
import com.piashmsu.tvapk.ui.screens.PlayerScreen
import com.piashmsu.tvapk.ui.screens.SearchScreen
import com.piashmsu.tvapk.ui.screens.SettingsScreen

private sealed class Tab(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Tab("home", "Home", Icons.Outlined.Home)
    data object Live : Tab("live", "Live TV", Icons.Outlined.LiveTv)
    data object Movies : Tab("movies", "Movies", Icons.Outlined.Movie)
    data object Search : Tab("search", "Search", Icons.Outlined.Search)
    data object Settings : Tab("settings", "Settings", Icons.Outlined.Settings)
}

private val tabs = listOf(Tab.Home, Tab.Live, Tab.Movies, Tab.Search, Tab.Settings)

@Composable
fun TvApkRoot() {
    val nav = rememberNavController()
    val openChannel: (Channel) -> Unit = { ch ->
        PlaybackTargetHolder.current.value = PlaybackTarget.LiveChannel(ch)
        nav.openPlayer()
    }
    val openMovie: (Movie) -> Unit = { m ->
        PlaybackTargetHolder.current.value = PlaybackTarget.VideoOnDemand(m)
        nav.openPlayer()
    }
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { TvApkBottomBar(nav) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B0F1F), Color(0xFF04060B))
                    )
                )
                .padding(padding)
        ) {
            NavHost(navController = nav, startDestination = Tab.Home.route) {
                composable(Tab.Home.route) {
                    HomeScreen(
                        onChannelTap = openChannel,
                        onMovieTap = openMovie,
                        onTabRequest = { route -> nav.tabNavigate(route) },
                    )
                }
                composable(Tab.Live.route) { LiveTvScreen(onChannelTap = openChannel) }
                composable(Tab.Movies.route) { MoviesScreen(onMovieTap = openMovie) }
                composable(Tab.Search.route) {
                    SearchScreen(onChannelTap = openChannel, onMovieTap = openMovie)
                }
                composable(Tab.Settings.route) { SettingsScreen() }
                composable("player") {
                    PlayerScreen(onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun TvApkBottomBar(nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    if (current == "player") return
    NavigationBar(
        containerColor = Color(0xCC0E1220),
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            val selected = current == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { nav.tabNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title, style = MaterialTheme.typography.labelLarge) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color(0x337C5CFF),
                    unselectedIconColor = Color(0xCCBFC4D6),
                    unselectedTextColor = Color(0xCCBFC4D6),
                )
            )
        }
    }
}

private fun NavHostController.tabNavigate(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.openPlayer() {
    navigate("player")
}
