package com.print3d.calculator.feature.machines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import com.print3d.calculator.domain.catalog.Catalog
import com.print3d.calculator.domain.model.Machine
import com.print3d.calculator.feature.materials.clean
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.AutoCompleteField
import com.print3d.calculator.ui.components.ConfirmDialog
import com.print3d.calculator.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachinesScreen(
    onBack: () -> Unit,
    vm: MachinesViewModel = hiltViewModel()
) {
    val machines by vm.machines.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Machine?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Machine?>(null) }
    var pendingDuplicate by remember { mutableStateOf<Machine?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_machines)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.new_machine)) }
            )
        }
    ) { pad ->
        if (machines.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.machines_empty),
                modifier = Modifier.padding(pad).fillMaxSize(),
                icon = Icons.Rounded.Print,
                actionLabel = stringResource(R.string.new_machine),
                onAction = { editing = null; showEditor = true }
            )
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(machines, key = { it.id }) { m ->
                    AppCard(modifier = Modifier.animateItem(), onClick = { editing = m; showEditor = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(m.name, style = MaterialTheme.typography.titleMedium)
                                val sub = listOf(m.brand, m.model).filter { it.isNotBlank() }.joinToString(" · ")
                                if (sub.isNotBlank() && sub != m.name) Text(sub, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { editing = m; showEditor = true }) {
                                Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { pendingDuplicate = m }) { Icon(Icons.Rounded.ContentCopy, null) }
                            IconButton(onClick = { pendingDelete = m }) {
                                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showEditor) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val initial = editing
        var name by remember { mutableStateOf(initial?.name ?: "") }
        var brand by remember { mutableStateOf(initial?.brand ?: "") }
        var model by remember { mutableStateOf(initial?.model ?: "") }
        var price by remember { mutableStateOf(initial?.price?.clean() ?: "") }
        var lifespan by remember { mutableStateOf(initial?.lifespanH?.clean() ?: "") }
        var power by remember { mutableStateOf(initial?.powerW?.clean() ?: "") }
        var hourCost by remember { mutableStateOf(initial?.hourCost?.clean() ?: "") }
        var notes by remember { mutableStateOf(initial?.notes ?: "") }

        ModalBottomSheet(onDismissRequest = { showEditor = false }, sheetState = sheetState) {
            Column(
                Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (initial == null) stringResource(R.string.new_machine) else stringResource(R.string.edit),
                    style = MaterialTheme.typography.headlineSmall
                )
                AutoCompleteField(
                    brand,
                    { brand = it; if (name.isBlank() && model.isNotBlank()) name = "$it $model".trim() },
                    stringResource(R.string.material_brand),
                    Catalog.printerBrandNames
                )
                AutoCompleteField(
                    model,
                    { model = it; if (name.isBlank() && brand.isNotBlank()) name = "$brand $it".trim() },
                    stringResource(R.string.machine_model),
                    Catalog.modelsForBrand(brand)
                )
                AppTextField(name, { name = it }, stringResource(R.string.machine_name), required = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(price, { price = it }, stringResource(R.string.machine_price), Modifier.weight(1f), numeric = true)
                    AppTextField(lifespan, { lifespan = it }, stringResource(R.string.machine_lifespan), Modifier.weight(1f), numeric = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(power, { power = it }, stringResource(R.string.machine_power), Modifier.weight(1f), numeric = true)
                    AppTextField(hourCost, { hourCost = it }, stringResource(R.string.machine_hour_cost), Modifier.weight(1f), numeric = true)
                }
                AppTextField(notes, { notes = it }, stringResource(R.string.machine_notes), singleLine = false)
                Button(
                    onClick = {
                        vm.save(
                            Machine(
                                id = initial?.id ?: 0,
                                name = name.trim().ifBlank { "Impresora" },
                                brand = brand.trim(),
                                model = model.trim(),
                                price = price.toDoubleOrNull() ?: 0.0,
                                lifespanH = lifespan.toDoubleOrNull() ?: 0.0,
                                powerW = power.toDoubleOrNull() ?: 0.0,
                                hourCost = hourCost.toDoubleOrNull() ?: 0.0,
                                notes = notes.trim()
                            )
                        )
                        showEditor = false
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.save)) }
                Spacer(Modifier.height(24.dp))
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
