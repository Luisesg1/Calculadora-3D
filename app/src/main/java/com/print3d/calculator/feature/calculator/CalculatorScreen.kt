package com.print3d.calculator.feature.calculator

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.domain.calc.CostKeys
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.ui.components.AppCard
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.AutoCompleteField
import com.print3d.calculator.ui.components.ResultRow
import com.print3d.calculator.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    quoteId: Long,
    onBack: () -> Unit,
    onRequireAuth: () -> Unit = {},
    vm: CalculatorViewModel = hiltViewModel(),
    monetization: com.print3d.calculator.feature.monetization.MonetizationViewModel = hiltViewModel()
) {
    val isSubscribed by monetization.isSubscribed.collectAsStateWithLifecycle()
    val materials by vm.materials.collectAsStateWithLifecycle()
    val machines by vm.machines.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val clientSuggestions by vm.clientSuggestions.collectAsStateWithLifecycle()
    val clients by vm.clients.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var costsExpanded by remember { mutableStateOf(false) }
    var showLimitUpsell by remember { mutableStateOf(false) }
    val templates by vm.templates.collectAsStateWithLifecycle()
    var showTemplateMenu by remember { mutableStateOf(false) }
    var showTemplateNameDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }
    var showTemplateLimit by remember { mutableStateOf(false) }
    // Which material line requested "+ new material" (opens the editor inline).
    var addMaterialForLine by remember { mutableStateOf<Int?>(null) }
    var showAddMachine by remember { mutableStateOf(false) }

    var form by remember { mutableStateOf(CalcForm(marginPct = "")) }
    var loadedId by remember { mutableStateOf(0L) }
    var initialized by remember { mutableStateOf(false) }

    // Seed defaults from settings once, or load an existing quote for edit/duplicate.
    androidx.compose.runtime.LaunchedEffect(quoteId, settings) {
        if (initialized) return@LaunchedEffect
        if (quoteId > 0) {
            vm.loadQuote(quoteId)?.let {
                form = CalcForm.from(it.input)
                loadedId = it.id
                initialized = true
            }
        } else {
            form = form.copy(
                marginPct = settings.defaultMargin.toString(),
                taxPct = settings.defaultTax.toString()
            )
            initialized = true
        }
    }

    val input = form.toInput()
    val machine = machines.firstOrNull { it.id == form.machineId }
    val result = vm.calculate(input, materials, machine, settings.electricityRate)
    // Joined material label for the PDF summary, e.g. "PLA+ · Negro + PETG · Rojo".
    val materialLabel = form.materialLines
        .mapNotNull { ln -> materials.firstOrNull { it.id == ln.materialId }?.displayLabel }
        .distinct().joinToString(" + ").ifBlank { null }
    val currency = settings.currency
    fun money(v: Double) = CurrencyFormatter.format(v, currency)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calc_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // TEMPLATES — reuse frequent jobs (Pro: unlimited, free: one).
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.layout.Box {
                        OutlinedButton(
                            onClick = { showTemplateMenu = true },
                            enabled = templates.isNotEmpty()
                        ) { Text(stringResource(R.string.use_template)) }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showTemplateMenu,
                            onDismissRequest = { showTemplateMenu = false }
                        ) {
                            templates.forEach { t ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(t.name) },
                                    onClick = {
                                        // Load the preset; keep it generic by dropping any saved client.
                                        form = CalcForm.from(t.input).copy(clientName = "", clientPhone = "", clientEmail = "")
                                        showTemplateMenu = false
                                    }
                                )
                            }
                        }
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        scope.launch {
                            if (!isSubscribed && vm.templateCount() >= com.print3d.calculator.feature.monetization.MonetizationViewModel.FREE_TEMPLATE_LIMIT) {
                                showTemplateLimit = true
                            } else {
                                templateName = form.projectName
                                showTemplateNameDialog = true
                            }
                        }
                    }) { Text(stringResource(R.string.save_as_template)) }
                }
            }

            // JOB
            item { SectionHeader(stringResource(R.string.section_job)) }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AutoCompleteField(
                            form.clientName,
                            { newName ->
                                val match = clients.firstOrNull { it.name.equals(newName, ignoreCase = true) }
                                form = form.copy(
                                    clientName = newName,
                                    clientPhone = match?.phone ?: form.clientPhone,
                                    clientEmail = match?.email ?: form.clientEmail
                                )
                            },
                            stringResource(R.string.field_client),
                            clientSuggestions
                        )
                        AppTextField(form.projectName, { form = form.copy(projectName = it) }, stringResource(R.string.field_project), required = true)
                        AppTextField(form.quantity, { form = form.copy(quantity = it) }, stringResource(R.string.field_quantity), numeric = true, required = true)
                        AppTextField(form.notes, { form = form.copy(notes = it) }, stringResource(R.string.field_notes), singleLine = false)
                    }
                }
            }

            // MATERIAL
            item { SectionHeader(stringResource(R.string.section_material)) }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DropdownField(
                            label = stringResource(R.string.field_select_machine),
                            selectedText = machine?.name ?: "—",
                            options = machines.map { it.id to it.name },
                            onSelect = { form = form.copy(machineId = it) },
                            addLabel = stringResource(R.string.new_machine),
                            onAddNew = { showAddMachine = true }
                        )

                        // One or more materials, each with its own grams.
                        form.materialLines.forEachIndexed { i, line ->
                            val selMat = materials.firstOrNull { it.id == line.materialId }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.weight(1f)) {
                                    DropdownField(
                                        label = stringResource(R.string.field_select_material),
                                        selectedText = selMat?.displayLabel ?: "—",
                                        options = materials.map { it.id to it.displayLabel },
                                        onSelect = { id ->
                                            form = form.copy(materialLines = form.materialLines.toMutableList().apply {
                                                this[i] = this[i].copy(materialId = id)
                                            })
                                        },
                                        addLabel = stringResource(R.string.new_material),
                                        onAddNew = { addMaterialForLine = i }
                                    )
                                }
                                AppTextField(
                                    line.grams,
                                    { g ->
                                        form = form.copy(materialLines = form.materialLines.toMutableList().apply {
                                            this[i] = this[i].copy(grams = g)
                                        })
                                    },
                                    stringResource(R.string.field_grams),
                                    Modifier.width(96.dp),
                                    numeric = true
                                )
                                if (form.materialLines.size > 1) {
                                    IconButton(onClick = {
                                        form = form.copy(materialLines = form.materialLines.toMutableList().apply { removeAt(i) })
                                    }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        androidx.compose.material3.TextButton(
                            onClick = { form = form.copy(materialLines = form.materialLines + MatLineForm()) }
                        ) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                            Text("  " + stringResource(R.string.add_material))
                        }
                    }
                }
            }

            // PRINT DATA
            item { SectionHeader(stringResource(R.string.section_print)) }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppTextField(form.meters, { form = form.copy(meters = it) }, stringResource(R.string.field_meters), Modifier.weight(1f), numeric = true, helpText = stringResource(R.string.help_meters))
                            Spacer(Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppTextField(form.printTimeH, { form = form.copy(printTimeH = it) }, stringResource(R.string.field_print_time), Modifier.weight(1f), numeric = true)
                            AppTextField(form.postTimeH, { form = form.copy(postTimeH = it) }, stringResource(R.string.field_post_time), Modifier.weight(1f), numeric = true, helpText = stringResource(R.string.help_postprocess))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppTextField(form.failurePct, { form = form.copy(failurePct = it) }, stringResource(R.string.field_failure), Modifier.weight(1f), numeric = true, helpText = stringResource(R.string.help_failures))
                            AppTextField(form.wastePct, { form = form.copy(wastePct = it) }, stringResource(R.string.field_waste), Modifier.weight(1f), numeric = true, helpText = stringResource(R.string.help_waste))
                        }
                    }
                }
            }

            // COSTS (optional — collapsed by default)
            item {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { costsExpanded = !costsExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(stringResource(R.string.section_costs), Modifier.weight(1f))
                        Icon(
                            if (costsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    AnimatedVisibility(costsExpanded) {
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CostKeys.BUILT_IN.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        pair.forEach { (key, res) ->
                                            val label = stringResource(resIdFor(res))
                                            AppTextField(
                                                value = form.builtInCosts[key] ?: "",
                                                onValueChange = {
                                                    form = form.copy(builtInCosts = form.builtInCosts.toMutableMap().apply { put(key, it) })
                                                },
                                                label = label,
                                                modifier = Modifier.weight(1f),
                                                numeric = true
                                            )
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MARGIN
            item { SectionHeader(stringResource(R.string.section_margin)) }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppTextField(form.marginPct, { form = form.copy(marginPct = it) }, stringResource(R.string.field_margin), Modifier.weight(1f), numeric = true, helpText = stringResource(R.string.help_margin))
                            AppTextField(form.taxPct, { form = form.copy(taxPct = it) }, stringResource(R.string.field_tax), Modifier.weight(1f), numeric = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppTextField(form.discountPct, { form = form.copy(discountPct = it) }, stringResource(R.string.field_discount), Modifier.weight(1f), numeric = true)
                            AppTextField(form.surchargePct, { form = form.copy(surchargePct = it) }, stringResource(R.string.field_surcharge), Modifier.weight(1f), numeric = true, helpText = stringResource(R.string.help_surcharge))
                        }
                        AppTextField(form.manualFinalPrice, { form = form.copy(manualFinalPrice = it) }, stringResource(R.string.field_final_price_manual), numeric = true, helpText = stringResource(R.string.help_manual_price))
                    }
                }
            }

            // RESULT
            item { SectionHeader(stringResource(R.string.section_result)) }
            item {
                AppCard {
                    Column {
                        ResultRow(stringResource(R.string.result_material), money(result.materialCost))
                        ResultRow(stringResource(R.string.result_electricity), money(result.electricityCost))
                        ResultRow(stringResource(R.string.result_machine), money(result.machineCost))
                        ResultRow(stringResource(R.string.result_labor), money(result.laborCost))
                        ResultRow(stringResource(R.string.result_extras), money(result.extrasCost))
                        ResultRow(
                            stringResource(R.string.result_profit),
                            money(result.profit) + "  (" + kotlin.math.round(result.profitPctOfPrice).toInt() + "%)"
                        )
                        if (result.discountAmount > 0) ResultRow(stringResource(R.string.result_discount), "-" + money(result.discountAmount))
                        if (result.taxAmount > 0) ResultRow(stringResource(R.string.result_tax), money(result.taxAmount))
                        Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                        ResultRow(stringResource(R.string.result_total), money(result.total), emphasize = true)
                        if (input.quantity > 1) {
                            Text(
                                stringResource(R.string.result_per_unit) + ": " + money(result.perUnit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val savedMsg = stringResource(R.string.quote_saved)
                    Button(
                        onClick = {
                            scope.launch {
                                // Free tier caps saved quotes; editing an existing one is always allowed.
                                val isNew = loadedId <= 0L
                                if (isNew && !isSubscribed &&
                                    vm.savedThisMonth() >= com.print3d.calculator.feature.monetization.MonetizationViewModel.FREE_QUOTES_PER_MONTH) {
                                    showLimitUpsell = true
                                    return@launch
                                }
                                vm.save(input, result, currency.code, loadedId)
                                // Toast survives the back navigation, unlike a snackbar on this screen.
                                android.widget.Toast.makeText(context, savedMsg, android.widget.Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        enabled = form.projectName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Save, null); Spacer(Modifier.height(0.dp))
                        Text("  " + stringResource(R.string.save_quote))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                // Export is free for everyone; revenue comes from periodic interstitials + subscription.
                                scope.launch {
                                    val quote = com.print3d.calculator.domain.model.Quotation(
                                        id = 0, number = vm.nextNumber(),
                                        createdAt = System.currentTimeMillis(),
                                        input = input, result = result, currencyCode = currency.code
                                    )
                                    // Logo + watermark-free PDF are subscriber perks.
                                    val exportSettings = if (isSubscribed) settings else settings.copy(logoUri = "")
                                    val f = com.print3d.calculator.feature.export.QuoteExporter.exportPdf(context, quote, exportSettings, materialLabel, pro = isSubscribed)
                                    com.print3d.calculator.feature.export.QuoteExporter.share(context, f, "application/pdf")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Rounded.PictureAsPdf, null); Text("  PDF") }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val quote = com.print3d.calculator.domain.model.Quotation(
                                        id = 0, number = vm.nextNumber(),
                                        createdAt = System.currentTimeMillis(),
                                        input = input, result = result, currencyCode = currency.code
                                    )
                                    val exportSettings = if (isSubscribed) settings else settings.copy(logoUri = "")
                                    val f = com.print3d.calculator.feature.export.QuoteExporter.exportPng(context, quote, exportSettings)
                                    com.print3d.calculator.feature.export.QuoteExporter.share(context, f, "image/png")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Rounded.Image, null); Text("  PNG") }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showLimitUpsell) {
        val activity = com.print3d.calculator.feature.monetization.findActivity(context)
        // Resolve strings in this composition — a Dialog sub-composition ignores the in-app locale.
        val limitTitle = stringResource(R.string.limit_title)
        val limitBody = stringResource(R.string.limit_msg, com.print3d.calculator.feature.monetization.MonetizationViewModel.FREE_QUOTES_PER_MONTH)
        val proCta = stringResource(R.string.pro_cta)
        val cancelTxt = stringResource(R.string.cancel)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLimitUpsell = false },
            title = { Text(limitTitle) },
            text = { Text(limitBody) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showLimitUpsell = false
                    activity?.let { monetization.subscribe(it) }
                }) { Text(proCta) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showLimitUpsell = false }) { Text(cancelTxt) }
            }
        )
    }

    if (showTemplateNameDialog) {
        val dTitle = stringResource(R.string.save_as_template)
        val dHint = stringResource(R.string.template_name)
        val dSave = stringResource(R.string.save)
        val dCancel = stringResource(R.string.cancel)
        val savedMsg = stringResource(R.string.template_saved)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTemplateNameDialog = false },
            title = { Text(dTitle) },
            text = {
                com.print3d.calculator.ui.components.AppTextField(templateName, { templateName = it }, dHint)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val n = templateName.trim().ifBlank { form.projectName.trim().ifBlank { "Plantilla" } }
                    scope.launch {
                        vm.saveTemplate(n, form.copy(clientName = "", clientPhone = "", clientEmail = "").toInput())
                        android.widget.Toast.makeText(context, savedMsg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    showTemplateNameDialog = false
                }) { Text(dSave) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTemplateNameDialog = false }) { Text(dCancel) }
            }
        )
    }

    if (showTemplateLimit) {
        val activity = com.print3d.calculator.feature.monetization.findActivity(context)
        val tTitle = stringResource(R.string.template_limit_title)
        val tBody = stringResource(R.string.template_limit_msg, com.print3d.calculator.feature.monetization.MonetizationViewModel.FREE_TEMPLATE_LIMIT)
        val tCta = stringResource(R.string.pro_cta)
        val tCancel = stringResource(R.string.cancel)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTemplateLimit = false },
            title = { Text(tTitle) },
            text = { Text(tBody) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showTemplateLimit = false
                    activity?.let { monetization.subscribe(it) }
                }) { Text(tCta) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTemplateLimit = false }) { Text(tCancel) }
            }
        )
    }

    // Inline "new printer" editor — save it and auto-select it, without leaving the quote.
    if (showAddMachine) {
        com.print3d.calculator.feature.machines.MachineEditorSheet(
            initial = null,
            onDismiss = { showAddMachine = false },
            onSave = { m ->
                scope.launch {
                    val newId = vm.saveMachine(m)
                    form = form.copy(machineId = newId)
                    showAddMachine = false
                }
            }
        )
    }

    // Inline "new material" editor — save it and auto-select it on the line that asked, without leaving the quote.
    addMaterialForLine?.let { lineIdx ->
        com.print3d.calculator.feature.materials.MaterialEditorSheet(
            initial = null,
            currencySymbol = currency.symbol,
            onDismiss = { addMaterialForLine = null },
            onSave = { mat ->
                scope.launch {
                    val newId = vm.saveMaterial(mat)
                    form = form.copy(materialLines = form.materialLines.toMutableList().apply {
                        if (lineIdx in indices) this[lineIdx] = this[lineIdx].copy(materialId = newId)
                    })
                    addMaterialForLine = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selectedText: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit,
    addLabel: String? = null,
    onAddNew: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("—") }, onClick = { expanded = false })
            }
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
            // Optional "+ create new" shortcut so you don't have to leave the quote.
            if (addLabel != null && onAddNew != null) {
                androidx.compose.material3.HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("  $addLabel", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = { expanded = false; onAddNew() }
                )
            }
        }
    }
}

private fun resIdFor(resName: String): Int = when (resName) {
    "field_labor" -> R.string.field_labor
    "field_packaging" -> R.string.field_packaging
    "field_shipping" -> R.string.field_shipping
    "field_supplies" -> R.string.field_supplies
    "field_paint" -> R.string.field_paint
    "field_sanding" -> R.string.field_sanding
    "field_glue" -> R.string.field_glue
    "field_magnets" -> R.string.field_magnets
    "field_screws" -> R.string.field_screws
    "field_bearings" -> R.string.field_bearings
    "field_electronics" -> R.string.field_electronics
    else -> R.string.result_extras
}
