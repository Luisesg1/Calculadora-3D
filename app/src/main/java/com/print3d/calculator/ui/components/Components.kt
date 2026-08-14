package com.print3d.calculator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Inbox
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.print3d.calculator.ui.theme.CardStyle
import com.print3d.calculator.ui.theme.Elevation
import com.print3d.calculator.ui.theme.Spacing
import com.print3d.calculator.ui.theme.layerLines

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    // Subtle press feedback on clickable cards — the tactile scale Linear/Stripe use.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardPressScale"
    )
    val clickMod = if (onClick != null) {
        Modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else Modifier
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickMod),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Elevation.none,
        shadowElevation = CardStyle.elevation,
        border = CardStyle.border
    ) {
        Box(Modifier.padding(Spacing.lg)) { content() }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String? = null,
    confirmLabel: String,
    dismissLabel: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = message?.let { { Text(it) } },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } }
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = Spacing.xs, top = Spacing.sm, bottom = Spacing.sm)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    required: Boolean = false,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = remember(value, options) {
        if (value.isBlank()) options
        else options.filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }
    }
    val open = expanded && filtered.isNotEmpty()
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(if (required) "$label *" else label) },
            singleLine = true,
            isError = isError,
            prefix = prefix?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { expanded = false }
        ) {
            filtered.take(8).forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onValueChange(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    prefix: String? = null,
    required: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    helpText: String? = null,
    /** Numeric fields only: max integer digits and decimals accepted. Keeps totals sane. */
    maxIntDigits: Int = 9,
    maxDecimals: Int = 2
) {
    OutlinedTextField(
        value = value,
        // Numeric fields reject letters/symbols and cap length: keep only digits + one decimal point.
        onValueChange = { raw ->
            onValueChange(if (numeric) sanitizeNumeric(raw, maxIntDigits, maxDecimals) else raw)
        },
        label = { Text(if (required) "$label *" else label) },
        singleLine = singleLine,
        isError = isError,
        prefix = prefix?.let { { Text(it) } },
        trailingIcon = helpText?.let { { HelpTooltip(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text
        ),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Keeps only digits and a single decimal separator ('.' or ','), normalized to '.'.
 * Caps the integer part to [maxIntDigits] and the fractional part to [maxDecimals] so
 * astronomical inputs (and the runaway totals they produce) are impossible to type.
 */
private fun sanitizeNumeric(raw: String, maxIntDigits: Int, maxDecimals: Int): String {
    val sb = StringBuilder()
    var hasDot = false
    var intDigits = 0
    var decDigits = 0
    for (c in raw) {
        when {
            c.isDigit() -> {
                if (!hasDot) {
                    if (intDigits < maxIntDigits) { sb.append(c); intDigits++ }
                } else {
                    if (decDigits < maxDecimals) { sb.append(c); decDigits++ }
                }
            }
            (c == '.' || c == ',') && !hasDot && maxDecimals > 0 -> { sb.append('.'); hasDot = true }
        }
    }
    return sb.toString()
}

/** A "?" icon that reveals [text] in a tooltip on tap — for explaining non-obvious fields. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpTooltip(text: String) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = { RichTooltip { Text(text) } },
        state = state
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(
                Icons.Rounded.HelpOutline,
                contentDescription = androidx.compose.ui.res.stringResource(com.print3d.calculator.R.string.help),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Inbox,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tinted primary halo behind the icon — reads intentional, not like a blank placeholder.
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .layerLines(MaterialTheme.colorScheme.primary, gap = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(Spacing.lg))
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xs))
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            androidx.compose.material3.Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
