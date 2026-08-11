package com.print3d.calculator.feature.materials

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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.print3d.calculator.R
import com.print3d.calculator.domain.catalog.Catalog
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.AutoCompleteField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialEditorSheet(
    initial: Material?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Material) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var brand by remember { mutableStateOf(initial?.brand ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: "") }
    var weight by remember { mutableStateOf(initial?.spoolWeightG?.clean() ?: "1000") }
    var price by remember { mutableStateOf(initial?.spoolPrice?.clean() ?: "") }
    var diameter by remember { mutableStateOf(initial?.diameterMm?.clean() ?: "1.75") }
    var density by remember { mutableStateOf(initial?.densityG?.clean() ?: "") }

    // The sheet renders in its own window that ignores the in-app locale override — re-provide
    // the localized context/config captured here (call site is localized) so strings stay translated.
    val localizedContext = androidx.compose.ui.platform.LocalContext.current
    val localizedConfig = androidx.compose.ui.platform.LocalConfiguration.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalContext provides localizedContext,
        androidx.compose.ui.platform.LocalConfiguration provides localizedConfig
      ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (initial == null) stringResource(R.string.new_material) else stringResource(R.string.edit),
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
            )
            val weightValid = (weight.toDoubleOrNull() ?: 0.0) > 0.0
            val priceValid = (price.toDoubleOrNull() ?: 0.0) > 0.0
            val valid = name.isNotBlank() && weightValid && priceValid

            AutoCompleteField(name, { name = it }, stringResource(R.string.material_name), Catalog.filamentTypes, required = true)
            AutoCompleteField(brand, { brand = it }, stringResource(R.string.material_brand), Catalog.filamentBrands)
            AppTextField(color, { color = it }, stringResource(R.string.material_color))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(weight, { weight = it }, stringResource(R.string.material_spool_weight), Modifier.weight(1f), numeric = true, required = true, isError = weight.isNotBlank() && !weightValid)
                AppTextField(price, { price = it }, stringResource(R.string.material_spool_price), Modifier.weight(1f), numeric = true, prefix = currencySymbol, required = true, isError = price.isNotBlank() && !priceValid)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(diameter, { diameter = it }, stringResource(R.string.material_diameter), Modifier.weight(1f), numeric = true)
                AppTextField(density, { density = it }, stringResource(R.string.material_density) + " (${stringResource(R.string.optional)})", Modifier.weight(1f), numeric = true)
            }
            Button(
                onClick = {
                    onSave(
                        Material(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            brand = brand.trim(),
                            color = color.trim(),
                            spoolWeightG = weight.toDoubleOrNull() ?: 1000.0,
                            spoolPrice = price.toDoubleOrNull() ?: 0.0,
                            diameterMm = diameter.toDoubleOrNull() ?: 1.75,
                            densityG = density.toDoubleOrNull()
                        )
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save)) }
            Spacer(Modifier.height(24.dp))
        }
      }
    }
}

fun Double.clean(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
