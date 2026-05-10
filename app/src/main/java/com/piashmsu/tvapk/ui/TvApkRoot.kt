package com.piashmsu.tvapk.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.piashmsu.tvapk.ui.screens.EpgTimelineScreen
import com.piashmsu.tvapk.ui.screens.PlayerScreen
import com.piashmsu.tvapk.ui.screens.SearchScreen
import com.piashmsu.tvapk.ui.screens.SettingsScreen
import com.piashmsu.tvapk.ui.theme.GradientBackground

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
                .background(GradientBackground)
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
                composable(Tab.Live.route) {
                    LiveTvScreen(
                        onChannelTap = openChannel,
                        onEpgTimeline = { nav.navigate("epg_timeline") },
                    )
                }
                composable(Tab.Movies.route) { MoviesScreen(onMovieTap = openMovie) }
                composable(Tab.Search.route) {
                    SearchScreen(onChannelTap = openChannel, onMovieTap = openMovie)
                }
                composable(Tab.Settings.route) { SettingsScreen() }
                composable("player") {
                    PlayerScreen(onBack = { nav.popBackStack() })
                }
                composable("epg_timeline") {
                    EpgTimelineScreen(
                        onBack = { nav.popBackStack() },
                        onChannelTap = openChannel,
                    )
                }
            }
        }
    }
}

/**
 * Floating glassmorphic pill nav. Selected tab grows a neon-purple capsule
 * under it with a faint glow. Replaces the stock Material NavigationBar so
 * we get the floating pill silhouette and the grow-on-select animation.
 */
@Composable
private fun TvApkBottomBar(nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    if (current == "player") return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xE6111436))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                NavPill(
                    tab = tab,
                    selected = current == tab.route,
                    onClick = { nav.tabNavigate(tab.route) },
                )
            }
        }
    }
}

@Composable
private fun NavPill(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val containerColor by animateColorAsState(
        if (selected) accent.copy(alpha = 0.18f) else Color.Transparent,
        label = "navPillBg",
    )
    val tint by animateColorAsState(
        if (selected) accent else Color(0xCCBFC4D6),
        label = "navPillTint",
    )
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(tab.icon, contentDescription = tab.title, tint = tint, modifier = Modifier.size(22.dp))
        if (selected) {
            Text(
                tab.title,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
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
