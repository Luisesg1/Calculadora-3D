package com.print3d.calculator.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.print3d.calculator.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Composable
fun AuthScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit = {},
    vm: AuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val signedIn by vm.isSignedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tab by remember { mutableStateOf(0) } // 0 = sign in, 1 = sign up
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(signedIn) { if (signedIn) onSignedIn() }
    LaunchedEffect(tab) { vm.clearMessages() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.onb_back))
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        TabRow(selectedTabIndex = tab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.onb_signin_title)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.auth_create_account)) })
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.business_email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_password)) },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
                }
            },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )

        // Sign-up extras: confirm field + live requirement checklist.
        AnimatedVisibility(tab == 1) {
            Column {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(stringResource(R.string.auth_confirm_password)) },
                    singleLine = true,
                    isError = confirm.isNotEmpty() && confirm != password,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Requirement(stringResource(R.string.auth_req_min), PasswordPolicy.hasMinLength(password))
                    Requirement(stringResource(R.string.auth_req_upper), PasswordPolicy.hasUppercase(password))
                    Requirement(stringResource(R.string.auth_req_lower), PasswordPolicy.hasLowercase(password))
                    Requirement(stringResource(R.string.auth_req_digit), PasswordPolicy.hasDigit(password))
                }
            }
        }

        ui.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(errorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        if (ui.needsConfirmation) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.auth_info_confirm_email),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (tab == 0) vm.signIn(email, password) else vm.signUp(email, password, confirm)
            },
            enabled = !ui.loading,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (ui.loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(
                    if (tab == 0) stringResource(R.string.onb_signin_title) else stringResource(R.string.auth_create_account),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                stringResource(R.string.auth_or),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = { vm.signInWithGoogle(context) },
            enabled = !ui.loading,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, Color(0xFFDADCE0)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1F1F1F)
            ),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.auth_google),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

/** Maps a machine-readable [AuthError] to its localized message. */
@Composable
private fun errorText(e: com.print3d.calculator.data.auth.AuthError): String = stringResource(
    when (e) {
        com.print3d.calculator.data.auth.AuthError.NETWORK -> R.string.auth_error_network
        com.print3d.calculator.data.auth.AuthError.EMAIL_TAKEN -> R.string.auth_error_email_taken
        com.print3d.calculator.data.auth.AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
        com.print3d.calculator.data.auth.AuthError.EMAIL_NOT_CONFIRMED -> R.string.auth_error_email_not_confirmed
        com.print3d.calculator.data.auth.AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
        com.print3d.calculator.data.auth.AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
        com.print3d.calculator.data.auth.AuthError.EMPTY_PASSWORD -> R.string.auth_error_empty_password
        com.print3d.calculator.data.auth.AuthError.PASSWORD_POLICY -> R.string.auth_error_password_policy
        com.print3d.calculator.data.auth.AuthError.PASSWORD_MISMATCH -> R.string.auth_error_password_mismatch
        com.print3d.calculator.data.auth.AuthError.UNKNOWN -> R.string.auth_error_unknown
    }
)

@Composable
private fun Requirement(label: String, met: Boolean) {
    val color = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val icon: ImageVector = Icons.Rounded.Check
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}
