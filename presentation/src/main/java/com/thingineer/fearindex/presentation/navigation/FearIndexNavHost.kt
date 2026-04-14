package com.thingineer.fearindex.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thingineer.fearindex.presentation.R
import com.thingineer.fearindex.presentation.feature.home.HomeScreen

enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int,
) {
    Home("home", Icons.Default.Home, R.string.tab_home),
    Chart("chart", Icons.Default.ShowChart, R.string.tab_chart),
    Community("community", Icons.Default.People, R.string.tab_community),
    Settings("settings", Icons.Default.Settings, R.string.tab_settings),
}

@Composable
fun FearIndexNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.Chart.route) { Text("Chart") }
            composable(BottomNavItem.Community.route) { Text("Community") }
            composable(BottomNavItem.Settings.route) { Text("Settings") }
        }
    }
}
