package com.print3d.calculator.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.domain.model.AppCurrency
import androidx.compose.ui.res.stringResource
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    vm: StatsViewModel = hiltViewModel(),
    monetization: com.print3d.calculator.feature.monetization.MonetizationViewModel = hiltViewModel()
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val isPro by monetization.isSubscribed.collectAsStateWithLifecycle()
    val currency = ui.settings.currency
    fun money(v: Double) = CurrencyFormatter.format(v, currency)
    fun pct(v: Double) = "${"%.0f".format(v)}%"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_stats)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangeChip(stringResource(R.string.range_month), ui.range == StatsRange.THIS_MONTH) { vm.setRange(StatsRange.THIS_MONTH) }
                    RangeChip(stringResource(R.string.range_3m), ui.range == StatsRange.LAST_3M) { vm.setRange(StatsRange.LAST_3M) }
                    RangeChip(stringResource(R.string.range_year), ui.range == StatsRange.THIS_YEAR) { vm.setRange(StatsRange.THIS_YEAR) }
                    RangeChip(stringResource(R.string.range_all), ui.range == StatsRange.ALL) { vm.setRange(StatsRange.ALL) }
                }
            }

            // Net profit hero card
            item {
                val positive = ui.data.profit >= 0
                val accent = if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = accent.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (positive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                null, tint = accent, modifier = Modifier.height(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.stats_profit), style = MaterialTheme.typography.titleMedium, color = accent)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(money(ui.data.profit), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = accent)
                    }
                }
            }

            // Income / Costs
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatBox(Modifier.weight(1f), stringResource(R.string.stats_income), money(ui.data.income))
                    StatBox(Modifier.weight(1f), stringResource(R.string.stats_costs), money(ui.data.costs))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatBox(Modifier.weight(1f), stringResource(R.string.stats_quotes), ui.data.quotes.toString())
                    StatBox(Modifier.weight(1f), stringResource(R.string.stats_avg), money(ui.data.avgTicket))
                }
            }

            // CRM funnel + inventory (advanced = Pro).
            item {
                Text(stringResource(R.string.stats_crm_title), style = MaterialTheme.typography.titleMedium)
            }
            if (isPro) {
                val d = ui.data
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_created), d.created.toString())
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_conversion), pct(d.conversionRate))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_accepted), d.accepted.toString())
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_rejected), d.rejected.toString())
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_pending), d.pending.toString())
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_in_production), d.inProduction.toString())
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_overdue), d.overdue.toString())
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_sales), money(d.salesFromQuotes))
                    }
                }
                item { Text(stringResource(R.string.stats_inventory_title), style = MaterialTheme.typography.titleMedium) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_most_used), d.mostUsedMaterial ?: "—")
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_top_consumption), d.topConsumptionMaterial ?: "—")
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_consumed_cost), money(d.materialsConsumedCost))
                        StatBox(Modifier.weight(1f), stringResource(R.string.stats_quote_profit), money(d.profitFromQuotes))
                    }
                }
            } else {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.stats_pro_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Monthly chart
            if (ui.data.months.isNotEmpty()) {
                item { ProfitChart(ui.data.months, currency) }
            } else {
                item {
                    Text(
                        stringResource(R.string.stats_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun StatBox(modifier: Modifier, label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = com.print3d.calculator.ui.theme.CardStyle.elevation,
        border = com.print3d.calculator.ui.theme.CardStyle.border,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfitChart(months: List<MonthBucket>, currency: AppCurrency) {
    val green = MaterialTheme.colorScheme.tertiary
    val red = MaterialTheme.colorScheme.error
    val axis = MaterialTheme.colorScheme.outline
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = com.print3d.calculator.ui.theme.CardStyle.elevation,
        border = com.print3d.calculator.ui.theme.CardStyle.border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.stats_profit), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            val maxAbs = max(1.0, months.maxOf { abs(it.profit) })
            Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                val n = months.size
                val slot = size.width / n
                val barW = slot * 0.5f
                val midY = size.height / 2f
                drawLine(axis, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1f)
                months.forEachIndexed { i, b ->
                    val h = (abs(b.profit) / maxAbs * (size.height / 2f - 8f)).toFloat()
                    val cx = slot * i + slot / 2f
                    val top = if (b.profit >= 0) midY - h else midY
                    drawRoundRect(
                        color = if (b.profit >= 0) green else red,
                        topLeft = Offset(cx - barW / 2f, top),
                        size = Size(barW, h.coerceAtLeast(2f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                months.forEach {
                    Text(it.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
