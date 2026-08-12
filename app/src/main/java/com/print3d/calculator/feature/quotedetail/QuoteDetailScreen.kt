package com.print3d.calculator.feature.quotedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.core.util.DateFormatter
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.DueState
import com.print3d.calculator.domain.model.QuoteStatus
import com.print3d.calculator.feature.export.QuoteExporter
import com.print3d.calculator.feature.monetization.MonetizationViewModel
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.ConfirmDialog
import com.print3d.calculator.ui.components.DuePill
import com.print3d.calculator.ui.components.ResultRow
import com.print3d.calculator.ui.components.StatusPill
import com.print3d.calculator.ui.components.statusColor
import com.print3d.calculator.ui.components.statusLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenClient: (Long) -> Unit,
    vm: QuoteDetailViewModel = hiltViewModel(),
    monetization: MonetizationViewModel = hiltViewModel()
) {
    val quote by vm.quote.collectAsStateWithLifecycle()
    val client by vm.client.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val materialLabel by vm.materialLabel.collectAsStateWithLifecycle()
    val isSubscribed by monetization.isSubscribed.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val q = quote

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(q?.number ?: stringResource(R.string.nav_quotes)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } },
                actions = {
                    if (q != null) {
                        IconButton(onClick = { onEdit(q.id) }) {
                            Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { pad ->
        if (q == null) {
            Spacer(Modifier.padding(pad))
            return@Scaffold
        }
        val cur = AppCurrency.fromCode(q.currencyCode)
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: project + status + due.
            Text(
                q.input.projectName.ifBlank { q.input.clientName.ifBlank { q.number } },
                style = MaterialTheme.typography.headlineSmall
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(q.status) { s -> vm.setStatus(s) }
                val due = q.dueState()
                if (due != DueState.NONE) DuePill(due)
            }

            // Amounts.
            AppCard {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.result_total), style = MaterialTheme.typography.titleMedium)
                        Text(
                            CurrencyFormatter.format(q.result.total, cur),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    ResultRow(stringResource(R.string.quote_profit), CurrencyFormatter.format(q.result.profit, cur))
                    ResultRow(stringResource(R.string.result_production_cost), CurrencyFormatter.format(q.result.productionCost, cur))
                }
            }

            // Client card.
            AppCard(onClick = client?.let { c -> { onOpenClient(c.id) } }) {
                Column {
                    Text(stringResource(R.string.crm_client), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(q.input.clientName.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
                    client?.rut?.takeIf { it.isNotBlank() }?.let {
                        Text("${stringResource(R.string.client_rut)}: $it", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val contact = listOf(q.input.clientPhone, q.input.clientEmail).filter { it.isNotBlank() }.joinToString(" · ")
                    if (contact.isNotBlank()) Text(contact, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (client != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.crm_view_client), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Due date row.
            AppCard(onClick = { showDatePicker = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.crm_due_date), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            q.dueDate?.let { DateFormatter.format(it, "dd/MM/yyyy") } ?: stringResource(R.string.crm_no_due),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Start production shortcut (auto-deducts stock for Pro).
            if (q.status == QuoteStatus.ACCEPTED) {
                Button(onClick = { vm.setStatus(QuoteStatus.IN_PRODUCTION) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Text("  " + stringResource(R.string.crm_start_production))
                }
            }

            // Share / PDF.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val exportSettings = if (isSubscribed) settings else settings.copy(logoUri = "")
                            val f = QuoteExporter.exportPdf(context, q, exportSettings, materialLabel, pro = isSubscribed)
                            QuoteExporter.share(context, f, "application/pdf")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Icon(Icons.Rounded.PictureAsPdf, null); Text("  " + stringResource(R.string.pdf)) }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val exportSettings = if (isSubscribed) settings else settings.copy(logoUri = "")
                            val f = QuoteExporter.exportPng(context, q, exportSettings)
                            QuoteExporter.share(context, f, "image/png")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Icon(Icons.Rounded.Image, null); Text("  PNG") }
            }

            // Timeline.
            Text(stringResource(R.string.crm_timeline), style = MaterialTheme.typography.titleMedium)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (events.isEmpty()) {
                        Text("${stringResource(R.string.crm_created)}: ${DateFormatter.format(q.createdAt, "dd/MM/yyyy HH:mm")}",
                            style = MaterialTheme.typography.bodyMedium)
                    } else {
                        events.forEach { e ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = statusColor(e.status))
                                Text(statusLabel(e.status), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(DateFormatter.format(e.timestamp, "dd/MM/yyyy HH:mm"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDelete && q != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete),
            message = stringResource(R.string.confirm_delete_msg),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = { vm.delete(onBack) },
            onDismiss = { showDelete = false }
        )
    }

    if (showDatePicker && q != null) {
        val state = rememberDatePickerState(initialSelectedDateMillis = q.dueDate)
        val okLabel = stringResource(R.string.save)
        val cancelLabel = stringResource(R.string.cancel)
        val clearLabel = stringResource(R.string.crm_no_due)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { vm.setDueDate(state.selectedDateMillis); showDatePicker = false }) { Text(okLabel) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.setDueDate(null); showDatePicker = false }) { Text(clearLabel) }
                    TextButton(onClick = { showDatePicker = false }) { Text(cancelLabel) }
                }
            }
        ) { DatePicker(state = state) }
    }
}
