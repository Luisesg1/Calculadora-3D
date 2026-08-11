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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.ConfirmDialog
import com.print3d.calculator.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    onBack: () -> Unit,
    vm: MaterialsViewModel = hiltViewModel()
) {
    val materials by vm.materials.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Material?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Material?>(null) }
    var pendingDuplicate by remember { mutableStateOf<Material?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_materials)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
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
                onAction = { editing = null; showEditor = true }
            )
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(materials, key = { it.id }) { m ->
                    Box(Modifier.animateItem()) {
                        MaterialRow(
                            m,
                            onClick = { editing = m; showEditor = true },
                            onEdit = { editing = m; showEditor = true },
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
private fun MaterialRow(
    m: Material,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.titleMedium)
                val sub = listOf(m.brand, m.color).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDuplicate) { Icon(Icons.Rounded.ContentCopy, null) }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
