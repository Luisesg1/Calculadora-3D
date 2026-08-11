package com.print3d.calculator.feature.machines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.print3d.calculator.R
import com.print3d.calculator.domain.catalog.Catalog
import com.print3d.calculator.domain.model.Machine
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.AutoCompleteField

/** Reusable printer editor — used by the Machines screen and inline from the calculator. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineEditorSheet(
    initial: Machine?,
    onDismiss: () -> Unit,
    onSave: (Machine) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var brand by remember { mutableStateOf(initial?.brand ?: "") }
    var model by remember { mutableStateOf(initial?.model ?: "") }
    var price by remember { mutableStateOf(initial?.price?.takeIf { it != 0.0 }?.toString() ?: "") }
    var lifespan by remember { mutableStateOf(initial?.lifespanH?.takeIf { it != 0.0 }?.toString() ?: "") }
    var power by remember { mutableStateOf(initial?.powerW?.takeIf { it != 0.0 }?.toString() ?: "") }
    var hourCost by remember { mutableStateOf(initial?.hourCost?.takeIf { it != 0.0 }?.toString() ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    // Re-provide the localized context so this popup keeps the in-app language.
    val localizedContext = LocalContext.current
    val localizedConfig = LocalConfiguration.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfig
        ) {
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
                        onSave(
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
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.save)) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
