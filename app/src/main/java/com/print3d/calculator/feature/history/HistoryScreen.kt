package com.print3d.calculator.feature.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.print3d.calculator.domain.model.DueState
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteStatus
import com.print3d.calculator.feature.monetization.MonetizationViewModel
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.ConfirmDialog
import com.print3d.calculator.ui.components.EmptyState
import com.print3d.calculator.ui.components.dueColor
import com.print3d.calculator.ui.components.dueLabel
import com.print3d.calculator.ui.components.statusColor
import com.print3d.calculator.ui.components.statusLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    vm: HistoryViewModel = hiltViewModel(),
    monetization: MonetizationViewModel = hiltViewModel()
) {
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    val filters by vm.filtersState.collectAsStateWithLifecycle()
    val clientNames by vm.clientNames.collectAsStateWithLifecycle()
    val allQuotes by vm.all.collectAsStateWithLifecycle()
    val isPro by monetization.isSubscribed.collectAsStateWithLifecycle()

    var kanban by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Quotation?>(null) }
    var pendingDuplicate by remember { mutableStateOf<Quotation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_quotes)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } },
                actions = {
                    // List / Kanban toggle. Kanban is Pro; free users just get a note.
                    IconButton(onClick = {
                        if (kanban) kanban = false
                        else if (isPro) kanban = true
                    }) {
                        Icon(
                            if (kanban) Icons.Rounded.ViewList else Icons.Rounded.ViewColumn,
                            contentDescription = stringResource(
                                if (kanban) R.string.quotes_view_list else R.string.quotes_view_kanban
                            ),
                            tint = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            AppTextField(
                value = filters.query,
                onValueChange = vm::setQuery,
                label = stringResource(R.string.search),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            FilterBar(vm, filters, clientNames)

            if (kanban && isPro) {
                KanbanBoard(allQuotes, onOpen, vm::setStatus)
            } else if (quotes.isEmpty()) {
                EmptyState(stringResource(R.string.history_empty), Modifier.fillMaxSize(), Icons.Rounded.Receipt)
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quotes, key = { it.id }) { q ->
                        QuoteCard(
                            q = q,
                            onOpen = { onOpen(q.id) },
                            onToggleFavorite = { vm.toggleFavorite(q) },
                            onDuplicate = { pendingDuplicate = q },
                            onDelete = { pendingDelete = q },
                            onSetStatus = { s -> vm.setStatus(q, s) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete),
            message = stringResource(R.string.confirm_delete_msg),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = { vm.delete(target) },
            onDismiss = { pendingDelete = null }
        )
    }
    pendingDuplicate?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_duplicate),
            confirmLabel = stringResource(R.string.duplicate),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = { vm.duplicate(target) },
            onDismiss = { pendingDuplicate = null }
        )
    }
}

@Composable
private fun FilterBar(vm: HistoryViewModel, filters: QuoteFilters, clientNames: List<String>) {
    // Row 1: sort + status chips.
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = vm::toggleSort) {
            Icon(
                Icons.Rounded.SwapVert,
                contentDescription = stringResource(R.string.sort_toggle),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        FilterChip(
            selected = filters.status == null,
            onClick = { vm.setStatusFilter(null) },
            label = { Text(stringResource(R.string.filter_all)) }
        )
        QuoteStatus.entries.forEach { s ->
            FilterChip(
                selected = filters.status == s,
                onClick = { vm.setStatusFilter(if (filters.status == s) null else s) },
                label = { Text(statusLabel(s)) }
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    // Row 2: date chips + client filter.
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val dates = listOf(
            DateFilter.ALL to R.string.date_all,
            DateFilter.THIS_MONTH to R.string.date_month,
            DateFilter.LAST_3M to R.string.date_3m,
            DateFilter.THIS_YEAR to R.string.date_year
        )
        dates.forEach { (d, res) ->
            FilterChip(
                selected = filters.date == d,
                onClick = { vm.setDateFilter(d) },
                label = { Text(stringResource(res)) }
            )
        }
        ClientFilterChip(filters.client, clientNames, vm::setClientFilter)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ClientFilterChip(selected: String?, clients: List<String>, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Resolve label outside the popup to keep the in-app locale.
    val allLabel = stringResource(R.string.filter_all)
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected ?: stringResource(R.string.filter_client)) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null, modifier = Modifier.size(16.dp)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(null); expanded = false })
            clients.forEach { c ->
                DropdownMenuItem(text = { Text(c) }, onClick = { onSelect(c); expanded = false })
            }
        }
    }
}

@Composable
private fun QuoteCard(
    q: Quotation,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onSetStatus: (QuoteStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val cur = AppCurrency.fromCode(q.currencyCode)
    AppCard(modifier = modifier, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    q.input.projectName.ifBlank { q.input.clientName.ifBlank { q.number } },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${q.number} · ${DateFormatter.format(q.createdAt, "dd/MM/yyyy")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        CurrencyFormatter.format(q.result.total, cur),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${stringResource(R.string.quote_profit)} ${CurrencyFormatter.format(q.result.profit, cur)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(q.status, onSetStatus)
                    val due = q.dueState()
                    if (due != DueState.NONE) DueBadge(due)
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (q.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    null,
                    tint = if (q.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpen) { Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDuplicate) { Icon(Icons.Rounded.ContentCopy, null) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

/** A colored pill showing the quote status; tap to change it via a dropdown. */
@Composable
private fun StatusBadge(status: QuoteStatus, onSet: (QuoteStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = statusColor(status)
    val entries = QuoteStatus.entries
    val labels = entries.map { statusLabel(it) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            color = color.copy(alpha = 0.14f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(statusLabel(status), style = MaterialTheme.typography.labelMedium, color = color)
                Icon(Icons.Rounded.ArrowDropDown, null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEachIndexed { i, s ->
                DropdownMenuItem(text = { Text(labels[i]) }, onClick = { onSet(s); expanded = false })
            }
        }
    }
}

@Composable
private fun DueBadge(due: DueState) {
    val color = dueColor(due)
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            dueLabel(due),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Horizontal Kanban board across the forward pipeline (Pro). Tap a card to open; ► advances it. */
@Composable
private fun KanbanBoard(
    quotes: List<Quotation>,
    onOpen: (Long) -> Unit,
    onSetStatus: (Quotation, QuoteStatus) -> Unit
) {
    Row(
        Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuoteStatus.KANBAN_FLOW.forEach { status ->
            val column = quotes.filter { it.status == status }
            KanbanColumn(status, column, onOpen, onSetStatus)
        }
    }
}

@Composable
private fun KanbanColumn(
    status: QuoteStatus,
    quotes: List<Quotation>,
    onOpen: (Long) -> Unit,
    onSetStatus: (Quotation, QuoteStatus) -> Unit
) {
    val color = statusColor(status)
    Column(Modifier.width(240.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(statusLabel(status), style = MaterialTheme.typography.titleSmall, color = color)
            Text("${quotes.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quotes, key = { it.id }) { q ->
                val cur = AppCurrency.fromCode(q.currencyCode)
                AppCard(onClick = { onOpen(q.id) }) {
                    Column {
                        Text(
                            q.input.projectName.ifBlank { q.input.clientName.ifBlank { q.number } },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            CurrencyFormatter.format(q.result.total, cur),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val next = q.status.next()
                        if (next != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { onSetStatus(q, next) }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        Icons.Rounded.ArrowForward,
                                        contentDescription = stringResource(R.string.crm_advance),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
