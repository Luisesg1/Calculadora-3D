package com.print3d.calculator.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.print3d.calculator.R
import com.print3d.calculator.domain.catalog.Catalog
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.AppLanguage

private const val STEPS = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    var step by remember { mutableIntStateOf(0) }
    var language by remember { mutableStateOf(AppLanguage.SYSTEM) }
    var currency by remember { mutableStateOf(AppCurrency.CLP) }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var manualMode by remember { mutableStateOf(false) }
    var otroMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    val printers = remember { androidx.compose.runtime.mutableStateListOf<Pair<String, String>>() }

    // Smart default: propose the currency for the device region (never converts money).
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val detected = AppCurrency.detectDefault()
        currency = detected
        vm.setCurrency(detected)
    }

    fun pendingPrinters(): List<Pair<String, String>> = buildList {
        addAll(printers)
        if (otroMode && customName.isNotBlank()) add(customName.trim() to "")
        else if (manualMode && (brand.isNotBlank() || model.isNotBlank())) add(brand to model)
    }
    val printerValid = pendingPrinters().isNotEmpty()
    fun finish() = vm.finish(pendingPrinters())

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ProgressHeader(step)

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(340)) { it } + fadeIn(tween(340))) togetherWith
                        (slideOutHorizontally(tween(340)) { -it / 3 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(340)) { -it } + fadeIn(tween(340))) togetherWith
                        (slideOutHorizontally(tween(340)) { it / 3 } + fadeOut(tween(200)))
                }
            },
            label = "onbStep",
            modifier = Modifier.weight(1f)
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            ) {
                when (s) {
                    0 -> {
                        StepHeader(Icons.Rounded.Language, stringResource(R.string.onb_language_title), stringResource(R.string.onb_language_sub))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SelectableCard("A", stringResource(R.string.onb_lang_auto), stringResource(R.string.onb_lang_auto_desc), language == AppLanguage.SYSTEM) { language = AppLanguage.SYSTEM; vm.setLanguage(AppLanguage.SYSTEM) }
                            SelectableCard("ES", stringResource(R.string.lang_es), stringResource(R.string.lang_es_desc), language == AppLanguage.SPANISH) { language = AppLanguage.SPANISH; vm.setLanguage(AppLanguage.SPANISH) }
                            SelectableCard("EN", stringResource(R.string.lang_en), stringResource(R.string.lang_en_desc), language == AppLanguage.ENGLISH) { language = AppLanguage.ENGLISH; vm.setLanguage(AppLanguage.ENGLISH) }
                            SelectableCard("PT", stringResource(R.string.lang_pt), stringResource(R.string.lang_pt_desc), language == AppLanguage.PORTUGUESE) { language = AppLanguage.PORTUGUESE; vm.setLanguage(AppLanguage.PORTUGUESE) }
                        }
                    }

                    1 -> {
                        StepHeader(Icons.Rounded.Payments, stringResource(R.string.onb_currency_title), stringResource(R.string.onb_currency_sub))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppCurrency.entries.forEach { c ->
                                CurrencyCard(c, currency == c) { currency = c; vm.setCurrency(c) }
                            }
                        }
                    }

                    else -> {
                        StepHeader(Icons.Rounded.Print, stringResource(R.string.onb_printer_title), stringResource(R.string.onb_printer_sub))
                        val popular = listOf("Bambu Lab", "Creality", "Anycubic", "Elegoo", "Prusa")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            popular.forEach { b ->
                                FilterChip(
                                    selected = brand == b && !manualMode && !otroMode,
                                    onClick = { brand = b; manualMode = false; otroMode = false },
                                    label = { Text(b) }
                                )
                            }
                            FilterChip(
                                selected = otroMode,
                                onClick = { otroMode = true; manualMode = false; brand = "" },
                                label = { Text(stringResource(R.string.other)) }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        when {
                            otroMode -> {
                                com.print3d.calculator.ui.components.AppTextField(
                                    customName, { customName = it }, stringResource(R.string.onb_custom_printer)
                                )
                            }
                            manualMode -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    com.print3d.calculator.ui.components.AutoCompleteField(brand, { brand = it }, stringResource(R.string.material_brand), Catalog.printerBrandNames)
                                    com.print3d.calculator.ui.components.AutoCompleteField(model, { model = it }, stringResource(R.string.machine_model), Catalog.modelsForBrand(brand))
                                    AssistChip(
                                        onClick = { if (brand.isNotBlank() || model.isNotBlank()) { printers.add(brand to model); brand = ""; model = "" } },
                                        label = { Text(stringResource(R.string.add)) },
                                        leadingIcon = { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                                    )
                                }
                            }
                            brand.isNotBlank() -> {
                                Text(stringResource(R.string.machine_model), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Catalog.modelsForBrand(brand).forEach { m ->
                                        val added = printers.any { it.first == brand && it.second == m }
                                        FilterChip(
                                            selected = added,
                                            onClick = { if (!added) printers.add(brand to m) },
                                            label = { Text(m) }
                                        )
                                    }
                                }
                            }
                        }
                        if (!manualMode && !otroMode) {
                            TextButton(onClick = { manualMode = true; brand = ""; otroMode = false }) { Text(stringResource(R.string.onb_printer_not_found)) }
                        }

                        if (printers.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                printers.forEachIndexed { i, p -> PrinterChip(p) { printers.removeAt(i) } }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        BottomBar(
            showBack = step > 0,
            onBack = { step-- },
            primaryLabel = if (step == STEPS - 1) stringResource(R.string.onb_finish) else stringResource(R.string.onb_next),
            primaryEnabled = step < STEPS - 1 || printerValid,
            onPrimary = { if (step < STEPS - 1) step++ else finish() }
        )
    }
}

/* ---------- Progress ---------- */

@Composable
private fun ProgressHeader(step: Int) {
    val target = (step + 1).toFloat() / STEPS
    val progress by animateFloatAsState(target, tween(400), label = "progress")
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 12.dp)) {
        Text(
            stringResource(R.string.onb_step_of, step + 1, STEPS),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

/* ---------- Step header ---------- */

@Composable
private fun StepHeader(icon: ImageVector, title: String, subtitle: String) {
    Spacer(Modifier.height(8.dp))
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 3.dp,
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
    }
    Spacer(Modifier.height(18.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}

/* ---------- Selectable card (language) ---------- */

@Composable
private fun SelectableCard(badge: String, title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(
                    if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(12.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(badge, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SelectionRing(selected)
        }
    }
}

@Composable
private fun SelectionRing(selected: Boolean) {
    val base = if (selected) {
        Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier.size(24.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
    }
    Box(base, contentAlignment = Alignment.Center) {
        AnimatedVisibility(selected, enter = scaleIn(spring()) + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

/* ---------- Currency card ---------- */

@Composable
private fun CurrencyCard(c: AppCurrency, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(
                    if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(10.dp)
                ),
                contentAlignment = Alignment.Center
            ) { Text(c.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.size(10.dp))
            Column {
                Text(c.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(c.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/* ---------- Printer chip in list ---------- */

@Composable
private fun PrinterChip(printer: Pair<String, String>, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val label = listOf(printer.first, printer.second).filter { it.isNotBlank() }.joinToString(" ")
            Text(label.ifBlank { "Impresora" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
            androidx.compose.material3.IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/* ---------- Bottom bar ---------- */

@Composable
private fun BottomBar(showBack: Boolean, onBack: () -> Unit, primaryLabel: String, primaryEnabled: Boolean, onPrimary: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showBack) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.onb_back)) }
            }
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed && primaryEnabled) 0.97f else 1f, spring(), label = "btnScale")
            Button(
                onClick = onPrimary,
                enabled = primaryEnabled,
                interactionSource = interaction,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                modifier = Modifier.weight(1f).height(54.dp).scale(scale)
            ) {
                Text(primaryLabel, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
