package com.print3d.calculator.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.ConfirmDialog
import com.print3d.calculator.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    vm: HistoryViewModel = hiltViewModel()
) {
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Quotation?>(null) }
    var pendingDuplicate by remember { mutableStateOf<Quotation?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_history)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            AppTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = stringResource(R.string.search),
                modifier = Modifier.padding(16.dp)
            )
            if (quotes.isEmpty()) {
                EmptyState(stringResource(R.string.history_empty), Modifier.fillMaxSize(), Icons.Rounded.Receipt)
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quotes, key = { it.id }) { q ->
                        val cur = AppCurrency.fromCode(q.currencyCode)
                        AppCard(modifier = Modifier.animateItem(), onClick = { onOpen(q.id) }) {
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
                                    Text(
                                        CurrencyFormatter.format(q.result.total, cur),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    StatusBadge(q.status) { s -> vm.setStatus(q, s) }
                                }
                                IconButton(onClick = { vm.toggleFavorite(q) }) {
                                    Icon(
                                        if (q.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        null,
                                        tint = if (q.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onOpen(q.id) }) {
                                    Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { pendingDuplicate = q }) { Icon(Icons.Rounded.ContentCopy, null) }
                                IconButton(onClick = { pendingDelete = q }) {
                                    Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
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
private fun statusLabel(s: com.print3d.calculator.domain.model.QuoteStatus): String = stringResource(
    when (s) {
        com.print3d.calculator.domain.model.QuoteStatus.DRAFT -> R.string.status_draft
        com.print3d.calculator.domain.model.QuoteStatus.SENT -> R.string.status_sent
        com.print3d.calculator.domain.model.QuoteStatus.ACCEPTED -> R.string.status_accepted
        com.print3d.calculator.domain.model.QuoteStatus.REJECTED -> R.string.status_rejected
    }
)

@Composable
private fun statusColor(s: com.print3d.calculator.domain.model.QuoteStatus): androidx.compose.ui.graphics.Color = when (s) {
    com.print3d.calculator.domain.model.QuoteStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
    com.print3d.calculator.domain.model.QuoteStatus.SENT -> MaterialTheme.colorScheme.primary
    com.print3d.calculator.domain.model.QuoteStatus.ACCEPTED -> MaterialTheme.colorScheme.tertiary
    com.print3d.calculator.domain.model.QuoteStatus.REJECTED -> MaterialTheme.colorScheme.error
}

/** A colored pill showing the quote status; tap to change it via a dropdown. */
@Composable
private fun StatusBadge(
    status: com.print3d.calculator.domain.model.QuoteStatus,
    onSet: (com.print3d.calculator.domain.model.QuoteStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val color = statusColor(status)
    // Resolve labels HERE (localized); the DropdownMenu is a popup and would fall back to device locale.
    val entries = com.print3d.calculator.domain.model.QuoteStatus.entries
    val labels = entries.map { statusLabel(it) }
    Box {
        androidx.compose.material3.Surface(
            onClick = { expanded = true },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
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
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEachIndexed { i, s ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(labels[i]) },
                    onClick = { onSet(s); expanded = false }
                )
            }
        }
    }
}
