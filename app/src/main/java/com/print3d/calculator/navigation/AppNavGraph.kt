package com.print3d.calculator.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.print3d.calculator.R
import com.print3d.calculator.feature.calculator.CalculatorScreen
import com.print3d.calculator.feature.clients.ClientsScreen
import com.print3d.calculator.feature.history.HistoryScreen
import com.print3d.calculator.feature.home.HomeScreen
import com.print3d.calculator.feature.machines.MachinesScreen
import com.print3d.calculator.feature.materials.MaterialsScreen
import com.print3d.calculator.feature.settings.SettingsScreen
import com.print3d.calculator.feature.stats.StatsScreen

private data class NavItem(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun AppNavGraph(
    monetization: com.print3d.calculator.feature.monetization.MonetizationViewModel = hiltViewModel()
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in Routes.TOP_LEVEL

    // One interstitial every SCREENS_PER_AD navigations, for non-subscribers. This is the single
    // Activity-scoped MonetizationViewModel, so the counter is global across the whole app.
    val adContext = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            com.print3d.calculator.feature.monetization.findActivity(adContext)
                ?.let { monetization.notifyScreenView(it) }
        }
    }

    // Creating/editing a quote is free; auth is only required at export time (handled in CalculatorScreen).
    fun openCalculator(quoteId: Long) {
        nav.navigate(Routes.calculator(quoteId))
    }
    fun openQuoteDetail(quoteId: Long) { nav.navigate(Routes.quoteDetail(quoteId)) }
    fun openClientDetail(clientId: Long) { nav.navigate(Routes.clientDetail(clientId)) }

    val items = listOf(
        NavItem(Routes.HOME, R.string.nav_home, Icons.Rounded.Home),
        NavItem(Routes.HISTORY, R.string.nav_quotes, Icons.Rounded.ReceiptLong),
        NavItem(Routes.MATERIALS, R.string.nav_materials, Icons.Rounded.Category),
        NavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(tonalElevation = 3.dp) {
                    items.forEach { item ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTab(nav, item.route) },
                            icon = { Icon(item.icon, null) },
                            label = { Text(stringResource(item.labelRes), maxLines = 1, softWrap = false) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { padding ->
        val dur = 260
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(dur)) + fadeIn(tween(dur)) },
            exitTransition = { fadeOut(tween(dur)) },
            popEnterTransition = { fadeIn(tween(dur)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(dur)) + fadeOut(tween(dur)) }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNewQuote = { openCalculator(-1L) },
                    onOpenQuote = { id -> openQuoteDetail(id) },
                    onNavigate = { route ->
                        if (route in Routes.TOP_LEVEL) navigateTab(nav, route) else nav.navigate(route)
                    },
                    onSignIn = { nav.navigate(Routes.AUTH) }
                )
            }
            composable(Routes.AUTH) {
                com.print3d.calculator.feature.auth.AuthScreen(
                    onBack = { nav.popBackStack() },
                    // Return to wherever auth was launched from (calculator export, settings) with state intact.
                    onSignedIn = { nav.popBackStack() }
                )
            }
            composable(
                route = Routes.CALCULATOR,
                arguments = listOf(navArgument("quoteId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val quoteId = entry.arguments?.getLong("quoteId") ?: -1L
                CalculatorScreen(
                    quoteId = quoteId,
                    onBack = { nav.popBackStack() },
                    onRequireAuth = { nav.navigate(Routes.AUTH) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { id -> openQuoteDetail(id) }
                )
            }
            composable(
                route = Routes.QUOTE_DETAIL,
                arguments = listOf(navArgument("quoteId") { type = NavType.LongType })
            ) {
                com.print3d.calculator.feature.quotedetail.QuoteDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> openCalculator(id) },
                    onOpenClient = { id -> openClientDetail(id) }
                )
            }
            composable(
                route = Routes.CLIENT_DETAIL,
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) {
                com.print3d.calculator.feature.clientdetail.ClientDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenQuote = { id -> openQuoteDetail(id) }
                )
            }
            composable(Routes.MATERIALS) { MaterialsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.MACHINES) { MachinesScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.CLIENTS) {
                ClientsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenClient = { id -> openClientDetail(id) }
                )
            }
            composable(Routes.STATS) { StatsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onSignIn = { nav.navigate(Routes.AUTH) }
                )
            }
        }
    }
}

private fun navigateTab(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
