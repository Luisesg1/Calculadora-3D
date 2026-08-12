package com.print3d.calculator.feature.clients

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
import androidx.compose.material.icons.rounded.People
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
import com.print3d.calculator.domain.model.Client
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.ConfirmDialog
import com.print3d.calculator.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onBack: () -> Unit,
    onOpenClient: (Long) -> Unit = {},
    vm: ClientsViewModel = hiltViewModel()
) {
    val clients by vm.clients.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Client?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Client?>(null) }
    var pendingDuplicate by remember { mutableStateOf<Client?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_clients)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.new_client)) }
            )
        }
    ) { pad ->
        if (clients.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.clients_empty),
                modifier = Modifier.padding(pad).fillMaxSize(),
                icon = Icons.Rounded.People,
                actionLabel = stringResource(R.string.new_client),
                onAction = { editing = null; showEditor = true }
            )
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(clients, key = { it.id }) { c ->
                    AppCard(modifier = Modifier.animateItem(), onClick = { onOpenClient(c.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium)
                                val sub = listOf(c.phone, c.email).filter { it.isNotBlank() }.joinToString(" · ")
                                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { editing = c; showEditor = true }) {
                                Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { pendingDuplicate = c }) { Icon(Icons.Rounded.ContentCopy, null) }
                            IconButton(onClick = { pendingDelete = c }) {
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
        var rut by remember { mutableStateOf(initial?.rut ?: "") }
        var phone by remember { mutableStateOf(initial?.phone ?: "") }
        var email by remember { mutableStateOf(initial?.email ?: "") }
        var address by remember { mutableStateOf(initial?.address ?: "") }
        var notes by remember { mutableStateOf(initial?.notes ?: "") }

        ModalBottomSheet(onDismissRequest = { showEditor = false }, sheetState = sheetState) {
            Column(
                Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (initial == null) stringResource(R.string.new_client) else stringResource(R.string.edit),
                    style = MaterialTheme.typography.headlineSmall
                )
                AppTextField(name, { name = it }, stringResource(R.string.client_name), required = true)
                AppTextField(rut, { rut = it }, stringResource(R.string.client_rut))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(phone, { phone = it }, stringResource(R.string.business_phone), Modifier.weight(1f))
                    AppTextField(email, { email = it }, stringResource(R.string.business_email), Modifier.weight(1f))
                }
                AppTextField(address, { address = it }, stringResource(R.string.business_address))
                AppTextField(notes, { notes = it }, stringResource(R.string.field_notes), singleLine = false)
                Button(
                    onClick = {
                        vm.save(
                            Client(
                                id = initial?.id ?: 0,
                                name = name.trim().ifBlank { "Cliente" },
                                rut = rut.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
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
