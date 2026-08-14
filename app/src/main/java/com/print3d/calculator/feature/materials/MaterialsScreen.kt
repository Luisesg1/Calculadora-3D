package com.print3d.calculator.feature.materials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.MovementReason
import com.print3d.calculator.domain.model.StockStatus
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.ConfirmDialog
import com.print3d.calculator.ui.components.EmptyState
import com.print3d.calculator.ui.components.movementReasonLabel
import com.print3d.calculator.core.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    onBack: () -> Unit,
    vm: MaterialsViewModel = hiltViewModel(),
    monetization: com.print3d.calculator.feature.monetization.MonetizationViewModel = hiltViewModel()
) {
    val materials by vm.materials.collectAsStateWithLifecycle()
    val lowStock by vm.lowStock.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()
    val isPro by monetization.isSubscribed.collectAsStateWithLifecycle()

    // Ask once for the notification permission (Android 13+) so low-stock alerts can reach the user.
    val context = androidx.compose.ui.platform.LocalContext.current
    val notifPermission = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var editing by remember { mutableStateOf<Material?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf<Material?>(null) }
    var showMaterialLimit by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Material?>(null) }
    var pendingDuplicate by remember { mutableStateOf<Material?>(null) }

    // Free tier caps how many materials can exist; editing existing ones is always allowed.
    val atMaterialLimit = !isPro &&
        materials.size >= com.print3d.calculator.feature.monetization.MonetizationViewModel.FREE_MATERIAL_LIMIT
    val startNewMaterial: () -> Unit = {
        if (atMaterialLimit) showMaterialLimit = true else { editing = null; showEditor = true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_materials)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = startNewMaterial,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.new_material)) }
            )
        }
    ) { pad ->
        if (materials.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.materials_empty),
                modifier = Modifier.padding(pad).fillMaxSize(),
                icon = Icons.Rounded.Category,
                actionLabel = stringResource(R.string.new_material),
                onAction = startNewMaterial
            )
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (lowStock.isNotEmpty()) {
                    item { LowStockCard(lowStock, currency) }
                }
                items(materials, key = { it.id }) { m ->
                    Box(Modifier.animateItem()) {
                        MaterialRow(
                            m, currency,
                            onClick = { editing = m; showEditor = true },
                            onEdit = { editing = m; showEditor = true },
                            onAdjust = { adjusting = m },
                            onDuplicate = { pendingDuplicate = m },
                            onDelete = { pendingDelete = m }
                        )
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showEditor) {
        MaterialEditorSheet(
            initial = editing,
            currencySymbol = currency.symbol,
            onDismiss = { showEditor = false },
            onSave = { vm.save(it); showEditor = false }
        )
    }

    adjusting?.let { target ->
        StockAdjustDialog(
            material = target,
            currency = currency,
            vm = vm,
            onDismiss = { adjusting = null }
        )
    }

    if (showMaterialLimit) {
        val title = stringResource(R.string.material_limit_title)
        val body = stringResource(R.string.material_limit_msg, com.print3d.calculator.feature.monetization.MonetizationViewModel.FREE_MATERIAL_LIMIT)
        val ok = stringResource(R.string.ok)
        AlertDialog(
            onDismissRequest = { showMaterialLimit = false },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = { TextButton(onClick = { showMaterialLimit = false }) { Text(ok) } }
        )
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
private fun LowStockCard(items: List<Material>, currency: AppCurrency) {
    val color = MaterialTheme.colorScheme.error
    AppCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Warning, null, tint = color)
                Text(stringResource(R.string.stock_low_title), style = MaterialTheme.typography.titleMedium, color = color)
            }
            Spacer(Modifier.height(6.dp))
            items.forEach { m ->
                Text(
                    stringResource(R.string.stock_low_msg, m.displayLabel, "${m.currentWeightG.g()} ${stringResource(R.string.grams_unit)}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MaterialRow(
    m: Material,
    currency: AppCurrency,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onAdjust: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.titleMedium)
                    val sub = listOf(m.brand, m.color).filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onAdjust) { Icon(Icons.Rounded.Inventory2, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDuplicate) { Icon(Icons.Rounded.ContentCopy, null) }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(8.dp))
            // Stock bar.
            val barColor = when (m.stockStatus) {
                StockStatus.OK -> MaterialTheme.colorScheme.tertiary
                StockStatus.LOW -> MaterialTheme.colorScheme.secondary
                StockStatus.OUT -> MaterialTheme.colorScheme.error
            }
            LinearProgressIndicator(
                progress = { m.stockPct.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${stringResource(R.string.stock_remaining)}: ${m.currentWeightG.g()} / ${m.spoolWeightG.g()} ${stringResource(R.string.grams_unit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.price_per_gram, CurrencyFormatter.format(m.pricePerGram, currency)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAdjustDialog(
    material: Material,
    currency: AppCurrency,
    vm: MaterialsViewModel,
    onDismiss: () -> Unit
) {
    var delta by remember { mutableStateOf("") }
    var subtract by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf(MovementReason.CORRECTION) }
    var reasonOpen by remember { mutableStateOf(false) }
    val movements by vm.movements(material.id).collectAsStateWithLifecycle(initialValue = emptyList())
    // Resolve reason labels outside the dropdown popup for correct in-app locale.
    val reasons = MovementReason.entries
    val reasonLabels = reasons.map { movementReasonLabel(it) }
    // Resolve ALL strings HERE (localized composition). An AlertDialog renders in its own
    // sub-composition that ignores the in-app locale override and would fall back to device locale.
    val gramsUnit = stringResource(R.string.grams_unit)
    val currentLabel = stringResource(R.string.stock_current)
    val deltaLabel = stringResource(R.string.stock_delta)
    val reasonHdr = stringResource(R.string.stock_reason)
    val movementsHdr = stringResource(R.string.stock_movements)
    val saveLabel = stringResource(R.string.save)
    val cancelLabel = stringResource(R.string.cancel)
    val selectedLabel = reasonLabels[reasons.indexOf(reason)]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(material.displayLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "$currentLabel: ${material.currentWeightG.g()} $gramsUnit",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = !subtract, onClick = { subtract = false }, label = { Text("+") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = subtract, onClick = { subtract = true }, label = { Text("−") }
                    )
                }
                AppTextField(delta, { delta = it }, deltaLabel, numeric = true)
                Box {
                    Surface(
                        onClick = { reasonOpen = true },
                        shape = RoundedCornerShape(12),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "$reasonHdr: $selectedLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                    DropdownMenu(expanded = reasonOpen, onDismissRequest = { reasonOpen = false }) {
                        reasons.forEachIndexed { i, r ->
                            DropdownMenuItem(text = { Text(reasonLabels[i]) }, onClick = { reason = r; reasonOpen = false })
                        }
                    }
                }
                if (movements.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(movementsHdr, style = MaterialTheme.typography.labelLarge)
                    movements.take(6).forEach { mv ->
                        val sign = if (mv.delta >= 0) "+" else ""
                        val rl = reasonLabels[reasons.indexOf(mv.reason)]
                        Text(
                            "${DateFormatter.format(mv.timestamp, "dd/MM")} · $rl · $sign${mv.delta.g()} → ${mv.newWeightG.g()} $gramsUnit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mag = delta.toDoubleOrNull()
                    if (mag != null && mag != 0.0) {
                        vm.adjustStock(material.id, if (subtract) -mag else mag, reason)
                    }
                    onDismiss()
                },
                enabled = (delta.toDoubleOrNull() ?: 0.0) != 0.0
            ) { Text(saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } }
    )
}

/** Grams, trimmed of a trailing .0 for display. */
private fun Double.g(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format("%.1f", this)
