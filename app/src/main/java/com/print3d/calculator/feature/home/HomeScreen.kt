package com.print3d.calculator.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    onNewQuote: () -> Unit,
    onOpenQuote: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    onSignIn: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
    authVm: com.print3d.calculator.feature.auth.AuthViewModel = hiltViewModel()
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val signedIn by authVm.isSignedIn.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showGuestLimit by remember { mutableStateOf(false) }
    // Guests get GuestQuotaManager.DAILY_LIMIT new quotes/day; signed-in users are unlimited.
    val startNewQuote: () -> Unit = {
        if (signedIn) onNewQuote()
        else scope.launch {
            if (vm.tryConsumeGuestQuote()) onNewQuote() else showGuestLimit = true
        }.let { }
    }

    val greeting = buildGreeting(ui.settings.businessName)
    val subtitle = if (ui.stats.quotesCount == 0)
        stringResource(R.string.home_subtitle_empty)
    else
        stringResource(R.string.home_month_summary, ui.stats.quotesThisMonth)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalPlayedStagger provides remember { mutableSetOf() }
    ) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Stagger(0) { HomeHeader(greeting, subtitle) } }

        item {
            Stagger(1) {
                StatsSummaryCard(
                    stats = ui.stats,
                    currency = ui.settings.currency,
                    labelQuotes = stringResource(R.string.stat_quotes),
                    labelIncome = stringResource(R.string.stat_income),
                    labelMaterial = stringResource(R.string.stat_material),
                    labelClients = stringResource(R.string.stat_clients)
                )
            }
        }

        item {
            Stagger(2) {
                PrimaryQuoteCard(
                    title = stringResource(R.string.menu_new_quote),
                    desc = stringResource(R.string.primary_new_quote_desc),
                    actionLabel = stringResource(R.string.primary_start),
                    onClick = startNewQuote
                )
            }
        }

        item { SectionTitle(stringResource(R.string.section_management)) }

        val tiles = listOf(
            Tile(Icons.Rounded.History, R.string.menu_history, R.string.menu_history_desc, "history"),
            Tile(Icons.Rounded.Category, R.string.menu_materials, R.string.menu_materials_desc, "materials"),
            Tile(Icons.Rounded.Print, R.string.menu_machines, R.string.menu_machines_desc, "machines"),
            Tile(Icons.Rounded.People, R.string.menu_clients, R.string.menu_clients_desc, "clients"),
            Tile(Icons.Rounded.BarChart, R.string.menu_stats, R.string.menu_stats_desc, "stats"),
            Tile(Icons.Rounded.Business, R.string.menu_business, R.string.menu_business_desc, "settings"),
            Tile(Icons.Rounded.Settings, R.string.menu_settings, R.string.menu_settings_desc, "settings")
        )
        // Management grid renders static — every tile must be visible immediately, no stagger.
        tiles.chunked(2).forEachIndexed { rowIndex, pair ->
            item(key = "tiles-$rowIndex") {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEach { t ->
                        SecondaryTile(
                            icon = t.icon,
                            title = stringResource(t.title),
                            desc = stringResource(t.desc),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(t.route) }
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        item {
            Stagger(7) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.section_recent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (ui.recent.isNotEmpty()) {
                        TextButton(onClick = { onNavigate("history") }) {
                            Text(stringResource(R.string.see_all))
                        }
                    }
                }
            }
        }

        if (ui.recent.isEmpty()) {
            item {
                Stagger(8) {
                    RecentEmptyState(
                        message = stringResource(R.string.recent_empty),
                        actionLabel = stringResource(R.string.create_first_quote),
                        onCreate = startNewQuote
                    )
                }
            }
        } else {
            item {
                Stagger(8) {
                    FeaturedLastQuote(
                        quote = ui.recent.first(),
                        dateFormat = ui.settings.dateFormat,
                        lastQuoteLabel = stringResource(R.string.last_quote),
                        clientLabel = stringResource(R.string.quote_client),
                        totalLabel = stringResource(R.string.label_total),
                        viewLabel = stringResource(R.string.view_details),
                        onClick = { onOpenQuote(ui.recent.first().id) }
                    )
                }
            }
            val rest = ui.recent.drop(1)
            if (rest.isNotEmpty()) {
                item {
                    Stagger(9) {
                        RecentActivityList(
                            quotes = rest,
                            dateFormat = ui.settings.dateFormat,
                            statusFavorite = stringResource(R.string.status_favorite),
                            statusSaved = stringResource(R.string.status_saved),
                            onOpen = onOpenQuote
                        )
                    }
                }
            }
        }
    }
    }

    if (showGuestLimit) {
        // Resolve strings HERE (localized context); a Dialog's own sub-composition would
        // fall back to the device locale and ignore the in-app language override.
        val titleTxt = stringResource(R.string.guest_limit_title)
        val msgTxt = stringResource(R.string.guest_limit_msg, com.print3d.calculator.data.quota.GuestQuotaManager.DAILY_LIMIT)
        val signInTxt = stringResource(R.string.onb_signin_title)
        val cancelTxt = stringResource(R.string.cancel)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGuestLimit = false },
            title = { Text(titleTxt) },
            text = { Text(msgTxt) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showGuestLimit = false; onSignIn() }) { Text(signInTxt) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showGuestLimit = false }) { Text(cancelTxt) }
            }
        )
    }
}

/** Indices whose entrance animation already played — kept per HomeScreen so scrolling a
 *  LazyColumn item off and back does NOT replay the fade/rise. */
private val LocalPlayedStagger = androidx.compose.runtime.compositionLocalOf { mutableSetOf<Int>() }

/** Staggered fade + rise-in on first appearance only. */
@Composable
private fun Stagger(index: Int, content: @Composable () -> Unit) {
    val played = LocalPlayedStagger.current
    val already = index in played
    var play by remember { mutableStateOf(already) }
    LaunchedEffect(Unit) {
        if (!already) {
            delay(index * 55L)
            play = true
            played.add(index)
        }
    }
    // First composition sets the value without animating, so replays start already visible.
    val progress by animateFloatAsState(if (play) 1f else 0f, tween(380), label = "stagger")
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 26.dp.toPx()
        }
    ) { content() }
}

private data class Tile(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: Int,
    val desc: Int,
    val route: String
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun buildGreeting(businessName: String): String {
    val base = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..18 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }
    return if (businessName.isBlank()) base else "$base, $businessName"
}
