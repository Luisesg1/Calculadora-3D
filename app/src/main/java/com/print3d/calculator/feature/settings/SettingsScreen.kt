package com.print3d.calculator.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.print3d.calculator.R
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.AppLanguage
import com.print3d.calculator.domain.model.AppThemeMode
import com.print3d.calculator.ui.components.AppCard
import androidx.compose.foundation.layout.imePadding
import com.print3d.calculator.ui.components.AppTextField
import com.print3d.calculator.ui.components.ConfirmDialog
import com.print3d.calculator.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
    authVm: com.print3d.calculator.feature.auth.AuthViewModel = hiltViewModel(),
    monetization: com.print3d.calculator.feature.monetization.MonetizationViewModel = hiltViewModel()
) {
    val isSubscribed by monetization.isSubscribed.collectAsStateWithLifecycle()
    val s = vm.settings.collectAsStateWithLifecycle().value
        ?: com.print3d.calculator.data.settings.AppSettings()
    val signedIn by authVm.isSignedIn.collectAsStateWithLifecycle()
    val userEmail by authVm.userEmail.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSignOut by remember { mutableStateOf(false) }

    val logoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            vm.setLogo(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionHeader(stringResource(R.string.settings_account)) }
            item {
                AppCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (signedIn) (userEmail ?: stringResource(R.string.onb_signin_title))
                                else stringResource(R.string.not_signed_in),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        if (signedIn) {
                            androidx.compose.material3.OutlinedButton(onClick = { showSignOut = true }) {
                                Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            androidx.compose.material3.Button(onClick = onSignIn) {
                                Text(stringResource(R.string.onb_signin_title))
                            }
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_premium)) }
            item {
                // Prices follow the selected currency; display-only (Play Console sets the real charge).
                // Annual = 10 months' price → "2 months free".
                val monthlyPrice = com.print3d.calculator.core.util.CurrencyFormatter.format(s.currency.proMonthly, s.currency)
                val annualPrice = com.print3d.calculator.core.util.CurrencyFormatter.format(s.currency.proMonthly * 10, s.currency)
                com.print3d.calculator.feature.monetization.ProUpsellCard(
                    isSubscribed = isSubscribed,
                    monthlyPrice = monthlyPrice,
                    annualPrice = annualPrice,
                    onSubscribe = { basePlan ->
                        com.print3d.calculator.feature.monetization.findActivity(context)?.let { monetization.subscribe(it, basePlan) }
                    }
                )
            }

            item { SectionHeader(stringResource(R.string.settings_appearance)) }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(stringResource(R.string.theme_system), s.theme == AppThemeMode.SYSTEM) { vm.setTheme(AppThemeMode.SYSTEM) }
                            ChoiceChip(stringResource(R.string.theme_light), s.theme == AppThemeMode.LIGHT) { vm.setTheme(AppThemeMode.LIGHT) }
                            ChoiceChip(stringResource(R.string.theme_dark), s.theme == AppThemeMode.DARK) { vm.setTheme(AppThemeMode.DARK) }
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_language)) }
            item {
                AppCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip("Auto", s.language == AppLanguage.SYSTEM) { vm.setLanguage(AppLanguage.SYSTEM) }
                        ChoiceChip("ES", s.language == AppLanguage.SPANISH) { vm.setLanguage(AppLanguage.SPANISH) }
                        ChoiceChip("EN", s.language == AppLanguage.ENGLISH) { vm.setLanguage(AppLanguage.ENGLISH) }
                        ChoiceChip("PT", s.language == AppLanguage.PORTUGUESE) { vm.setLanguage(AppLanguage.PORTUGUESE) }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_currency)) }
            item {
                AppCard {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppCurrency.entries.forEach { c ->
                            ChoiceChip(c.code, s.currency == c) { vm.setCurrency(c) }
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_defaults)) }
            item {
                var elec by remember(s.electricityRate) { mutableStateOf(cleanNum(s.electricityRate)) }
                var margin by remember(s.defaultMargin) { mutableStateOf(cleanNum(s.defaultMargin)) }
                var tax by remember(s.defaultTax) { mutableStateOf(cleanNum(s.defaultTax)) }
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppTextField(elec, { elec = it; it.toDoubleOrNull()?.let(vm::setElectricity) }, stringResource(R.string.settings_electricity_cost), numeric = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppTextField(margin, { margin = it; it.toDoubleOrNull()?.let(vm::setMargin) }, stringResource(R.string.settings_default_margin), Modifier.weight(1f), numeric = true)
                            AppTextField(tax, { tax = it; it.toDoubleOrNull()?.let(vm::setTax) }, stringResource(R.string.settings_default_tax), Modifier.weight(1f), numeric = true)
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_business)) }
            item {
                var name by remember(s.businessName) { mutableStateOf(s.businessName) }
                var phone by remember(s.businessPhone) { mutableStateOf(s.businessPhone) }
                var email by remember(s.businessEmail) { mutableStateOf(s.businessEmail) }
                var ig by remember(s.businessInstagram) { mutableStateOf(s.businessInstagram) }
                var fb by remember(s.businessFacebook) { mutableStateOf(s.businessFacebook) }
                var web by remember(s.businessWeb) { mutableStateOf(s.businessWeb) }
                var addr by remember(s.businessAddress) { mutableStateOf(s.businessAddress) }
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (s.logoUri.isNotBlank()) {
                                AsyncImage(
                                    model = s.logoUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).let { it }
                                )
                            } else {
                                Icon(Icons.Rounded.Image, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            // Company logo is a subscriber perk. Non-subscribers tapping it go to subscribe.
                            if (isSubscribed) {
                                OutlinedButton(onClick = { logoPicker.launch(arrayOf("image/*")) }) {
                                    Text(stringResource(R.string.pick_logo))
                                }
                            } else {
                                OutlinedButton(onClick = {
                                    com.print3d.calculator.feature.monetization.findActivity(context)?.let { monetization.subscribe(it) }
                                }) {
                                    Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp))
                                    Text("  " + stringResource(R.string.logo_locked))
                                }
                            }
                        }
                        AppTextField(name, { name = it }, stringResource(R.string.business_name))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppTextField(phone, { phone = it }, stringResource(R.string.business_phone), Modifier.weight(1f))
                            AppTextField(email, { email = it }, stringResource(R.string.business_email), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppTextField(ig, { ig = it }, stringResource(R.string.business_instagram), Modifier.weight(1f))
                            AppTextField(fb, { fb = it }, stringResource(R.string.business_facebook), Modifier.weight(1f))
                        }
                        AppTextField(web, { web = it }, stringResource(R.string.business_web))
                        AppTextField(addr, { addr = it }, stringResource(R.string.business_address), singleLine = false)

                        // Explicit save with confirmation — only enabled when there are edits.
                        val dirty = name != s.businessName || phone != s.businessPhone ||
                            email != s.businessEmail || ig != s.businessInstagram ||
                            fb != s.businessFacebook || web != s.businessWeb || addr != s.businessAddress
                        val savedMsg = stringResource(R.string.settings_saved)
                        var showSaveConfirm by remember { mutableStateOf(false) }
                        Button(
                            onClick = { showSaveConfirm = true },
                            enabled = dirty,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.settings_save)) }

                        if (showSaveConfirm) {
                            ConfirmDialog(
                                title = stringResource(R.string.settings_save_confirm_title),
                                message = stringResource(R.string.settings_save_confirm_msg),
                                confirmLabel = stringResource(R.string.save),
                                dismissLabel = stringResource(R.string.cancel),
                                onConfirm = {
                                    showSaveConfirm = false
                                    vm.setBusiness(name, phone, email, ig, fb, web, addr)
                                    scope.launch { snackbar.showSnackbar(savedMsg) }
                                },
                                onDismiss = { showSaveConfirm = false }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showSignOut) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_sign_out),
            message = stringResource(R.string.confirm_sign_out_msg),
            confirmLabel = stringResource(R.string.sign_out),
            dismissLabel = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = { authVm.signOut(); showSignOut = false },
            onDismiss = { showSignOut = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private fun cleanNum(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
