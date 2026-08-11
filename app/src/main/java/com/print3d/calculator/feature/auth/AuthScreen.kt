package com.print3d.calculator.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.R
import com.print3d.calculator.ui.theme.AppColors
import com.print3d.calculator.ui.theme.IndigoGradientTip

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

    // One-shot entrance animation for the whole content.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterAlpha by animateFloatAsState(
        if (entered) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "enterAlpha"
    )
    val enterOffset by animateDpAsState(
        if (entered) 0.dp else 18.dp, tween(450, easing = FastOutSlowInEasing), label = "enterOffset"
    )

    val cs = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(cs.background)
            // Very soft violet aura behind the header — reads as "tech", not decoration.
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cs.primary.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.10f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width / 2f, size.height * 0.10f),
                    radius = size.width * 0.85f
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    alpha = enterAlpha
                    translationY = enterOffset.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.onb_back),
                        tint = cs.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---- Branding ----
            BrandLogo(size = 76.dp)
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.auth_brand_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(R.string.auth_brand_descriptor),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = cs.primary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(26.dp))

            // ---- Segmented control ----
            SegmentedTabs(
                selected = tab,
                onSelect = { tab = it },
                left = stringResource(R.string.onb_signin_title),
                right = stringResource(R.string.auth_create_account)
            )

            Spacer(Modifier.height(22.dp))

            // ---- Inputs ----
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.auth_email_label),
                placeholder = stringResource(R.string.auth_email_placeholder),
                leadingIcon = Icons.Rounded.Email,
                keyboardType = KeyboardType.Email,
                isValid = email.isNotEmpty() && isValidEmail(email)
            )
            Spacer(Modifier.height(14.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.auth_password),
                placeholder = "••••••••",
                leadingIcon = Icons.Rounded.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword }
            )

            // Sign-up extras: confirm + live requirement checklist.
            AnimatedVisibility(
                visible = tab == 1,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    AuthTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = stringResource(R.string.auth_confirm_password),
                        placeholder = "••••••••",
                        leadingIcon = Icons.Rounded.Lock,
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        isError = confirm.isNotEmpty() && confirm != password,
                        isValid = confirm.isNotEmpty() && confirm == password
                    )
                    Spacer(Modifier.height(14.dp))
                    PasswordRequirements(password)
                }
            }

            // ---- Messages ----
            AnimatedVisibility(ui.error != null, enter = fadeIn(), exit = fadeOut()) {
                ui.error?.let {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            errorText(it),
                            color = cs.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            AnimatedVisibility(ui.needsConfirmation, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.auth_info_confirm_email),
                        color = cs.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- Primary button ----
            PrimaryGradientButton(
                text = if (tab == 0) stringResource(R.string.onb_signin_title)
                else stringResource(R.string.auth_create_account),
                loading = ui.loading,
                onClick = {
                    if (tab == 0) vm.signIn(email, password)
                    else vm.signUp(email, password, confirm)
                }
            )

            Spacer(Modifier.height(20.dp))

            // ---- Divider ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HairlineDivider(Modifier.weight(1f))
                Text(
                    stringResource(R.string.auth_or_continue),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HairlineDivider(Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // ---- Google ----
            GoogleButton(
                enabled = !ui.loading,
                onClick = { vm.signInWithGoogle(context) }
            )

            Spacer(Modifier.height(28.dp))

            // ---- Bottom branding ----
            BrandFooter(tab = tab)

            Spacer(Modifier.height(20.dp))
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Brand logo — isometric 3D cube (print bed grid + nozzle)          */
/* ------------------------------------------------------------------ */

@Composable
private fun BrandLogo(size: androidx.compose.ui.unit.Dp) {
    // The real app launcher icon (adaptive icon: background color + foreground),
    // clipped to a rounded square so it matches the icon on the home screen.
    Image(
        painter = painterResource(R.mipmap.ic_launcher),
        contentDescription = stringResource(R.string.auth_logo_cd),
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(22.dp))
    )
}

/* ------------------------------------------------------------------ */
/*  Segmented control                                                  */
/* ------------------------------------------------------------------ */

@Composable
private fun SegmentedTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    left: String,
    right: String
) {
    val cs = MaterialTheme.colorScheme
    val bias by animateFloatAsState(
        if (selected == 0) -1f else 1f,
        spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "segBias"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, cs.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        // Moving indicator
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .align(BiasAlignment(bias, 0f))
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.horizontalGradient(listOf(cs.primary, IndigoGradientTip))
                )
        )
        Row(Modifier.fillMaxSize()) {
            SegmentLabel(left, selected == 0, Modifier.weight(1f)) { onSelect(0) }
            SegmentLabel(right, selected == 1, Modifier.weight(1f)) { onSelect(1) }
        }
    }
}

@Composable
private fun SegmentLabel(
    text: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val color by animateColorAsState(
        if (active) cs.onPrimary else cs.onSurfaceVariant, tween(200), label = "segTxt"
    )
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(11.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = color
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Premium text field                                                 */
/* ------------------------------------------------------------------ */

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    isError: Boolean = false,
    isValid: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        when {
            isError -> cs.error
            focused -> cs.primary
            isValid -> AppColors.success
            else -> cs.outline
        }, tween(180), label = "fieldBorder"
    )
    val glow by animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "fieldGlow")
    val iconTint by animateColorAsState(
        when {
            isError -> cs.error
            focused -> cs.primary
            else -> cs.onSurfaceVariant
        }, tween(180), label = "fieldIcon"
    )

    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (focused) cs.primary else cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surface)
                // Subtle focus glow ring.
                .border(
                    width = if (focused || isError || isValid) 1.5.dp else 1.dp,
                    color = borderColor.copy(alpha = if (glow > 0f) 1f else 0.9f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(leadingIcon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface)
                    ),
                    cursorBrush = Brush.verticalGradient(listOf(cs.primary, cs.primary)),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (isPassword && !showPassword)
                        PasswordVisualTransformation() else VisualTransformation.None,
                    interactionSource = interaction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword && onTogglePassword != null) {
                IconButton(onClick = onTogglePassword, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (isValid) {
                Icon(
                    Icons.Rounded.CheckCircle, null,
                    tint = AppColors.success, modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Password requirements                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun PasswordRequirements(password: String) {
    val allGood = PasswordPolicy.isValid(password)
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedContent(
            targetState = allGood,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "reqBlock"
        ) { good ->
            if (good) {
                Requirement(stringResource(R.string.auth_all_good), true)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Requirement(stringResource(R.string.auth_req_min), PasswordPolicy.hasMinLength(password))
                    Requirement(stringResource(R.string.auth_req_upper), PasswordPolicy.hasUppercase(password))
                    Requirement(stringResource(R.string.auth_req_lower), PasswordPolicy.hasLowercase(password))
                    Requirement(stringResource(R.string.auth_req_digit), PasswordPolicy.hasDigit(password))
                }
            }
        }
    }
}

@Composable
private fun Requirement(label: String, met: Boolean) {
    val color by animateColorAsState(
        if (met) AppColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "reqColor"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (met) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null, tint = color, modifier = Modifier.size(16.dp)
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/* ------------------------------------------------------------------ */
/*  Primary gradient button                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun PrimaryGradientButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.6f, stiffness = 700f), label = "btnScale"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(cs.primary, IndigoGradientTip)))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !loading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = cs.onPrimary
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward, null,
                    tint = cs.onPrimary, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Google button                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun GoogleButton(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.6f, stiffness = 700f), label = "gBtnScale"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F1F1F)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Bottom branding footer                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun BrandFooter(tab: Int) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hairline separator above the footer.
        Box(
            Modifier
                .width(36.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cs.primary.copy(alpha = 0.4f))
        )
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "footerTag"
        ) { t ->
            Text(
                stringResource(if (t == 0) R.string.auth_tagline_login else R.string.auth_tagline_signup),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = cs.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.titleLarge.lineHeight
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.auth_tagline_caption),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Misc                                                               */
/* ------------------------------------------------------------------ */

@Composable
private fun HairlineDivider(modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .height(1.dp)
            .background(cs.outline.copy(alpha = 0.6f))
    )
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
