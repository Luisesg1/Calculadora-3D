package com.print3d.calculator.feature.clientdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.core.util.DateFormatter
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.statusColor
import com.print3d.calculator.ui.components.statusLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    onBack: () -> Unit,
    onOpenQuote: (Long) -> Unit,
    vm: ClientDetailViewModel = hiltViewModel()
) {
    val client by vm.client.collectAsStateWithLifecycle()
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.name ?: stringResource(R.string.menu_clients)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } }
            )
        }
    ) { pad ->
        val c = client
        // Use the currency of the first quote for the "total bought" figure; fall back to CLP.
        val cur = AppCurrency.fromCode(quotes.firstOrNull()?.currencyCode)
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Column {
                        Text(c?.name ?: "—", style = MaterialTheme.typography.titleLarge)
                        c?.rut?.takeIf { it.isNotBlank() }?.let {
                            Text("${stringResource(R.string.client_rut)}: $it",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val contact = listOfNotNull(c?.phone, c?.email).filter { it.isNotBlank() }.joinToString(" · ")
                        if (contact.isNotBlank()) Text(contact, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiTile(stringResource(R.string.client_total_bought), CurrencyFormatter.format(stats.totalBought, cur), Modifier.weight(1f))
                    KpiTile(stringResource(R.string.client_quotes_count), stats.quotesCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiTile(stringResource(R.string.client_accepted), stats.accepted.toString(), Modifier.weight(1f))
                    KpiTile(stringResource(R.string.client_rejected), stats.rejected.toString(), Modifier.weight(1f))
                }
            }
            item {
                Text(stringResource(R.string.client_history), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }
            items(quotes, key = { it.id }) { q ->
                val qcur = AppCurrency.fromCode(q.currencyCode)
                AppCard(onClick = { onOpenQuote(q.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(q.input.projectName.ifBlank { q.number }, style = MaterialTheme.typography.titleMedium)
                            Text("${q.number} · ${DateFormatter.format(q.createdAt, "dd/MM/yyyy")}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(statusLabel(q.status), style = MaterialTheme.typography.labelMedium, color = statusColor(q.status))
                        }
                        Text(CurrencyFormatter.format(q.result.total, qcur),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun KpiTile(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
