package com.print3d.calculator.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.print3d.calculator.R
import com.print3d.calculator.domain.model.DueState
import com.print3d.calculator.domain.model.MovementReason
import com.print3d.calculator.domain.model.QuoteStatus

/** Localized label for a quote status. Resolve OUTSIDE popups (device-locale bug). */
@Composable
fun statusLabel(s: QuoteStatus): String = stringResource(
    when (s) {
        QuoteStatus.DRAFT -> R.string.status_draft
        QuoteStatus.SENT -> R.string.status_sent
        QuoteStatus.VIEWED -> R.string.status_viewed
        QuoteStatus.ACCEPTED -> R.string.status_accepted
        QuoteStatus.IN_PRODUCTION -> R.string.status_production
        QuoteStatus.DELIVERED -> R.string.status_delivered
        QuoteStatus.REJECTED -> R.string.status_rejected
        QuoteStatus.CANCELLED -> R.string.status_cancelled
    }
)

/** Consistent status color across History, Kanban, detail and stats. */
@Composable
fun statusColor(s: QuoteStatus): Color = when (s) {
    QuoteStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
    QuoteStatus.SENT -> MaterialTheme.colorScheme.primary
    QuoteStatus.VIEWED -> MaterialTheme.colorScheme.secondary
    QuoteStatus.ACCEPTED -> MaterialTheme.colorScheme.tertiary
    QuoteStatus.IN_PRODUCTION -> MaterialTheme.colorScheme.primary
    QuoteStatus.DELIVERED -> MaterialTheme.colorScheme.tertiary
    QuoteStatus.REJECTED -> MaterialTheme.colorScheme.error
    QuoteStatus.CANCELLED -> MaterialTheme.colorScheme.error
}

@Composable
fun dueLabel(s: DueState): String = stringResource(
    when (s) {
        DueState.VALID -> R.string.due_valid
        DueState.DUE_SOON -> R.string.due_soon
        DueState.OVERDUE -> R.string.due_overdue
        DueState.NONE -> R.string.due_valid
    }
)

@Composable
fun dueColor(s: DueState): Color = when (s) {
    DueState.VALID -> MaterialTheme.colorScheme.tertiary
    DueState.DUE_SOON -> MaterialTheme.colorScheme.secondary
    DueState.OVERDUE -> MaterialTheme.colorScheme.error
    DueState.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Colored status pill with a dropdown to change the status. Shared by list and detail. */
@Composable
fun StatusPill(status: QuoteStatus, onSet: (QuoteStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = statusColor(status)
    val entries = QuoteStatus.entries
    val labels = entries.map { statusLabel(it) }
    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(statusLabel(status), style = MaterialTheme.typography.labelLarge, color = color)
            Icon(Icons.Rounded.ArrowDropDown, null, tint = color, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEachIndexed { i, s ->
                DropdownMenuItem(text = { Text(labels[i]) }, onClick = { onSet(s); expanded = false })
            }
        }
    }
}

/** Read-only colored pill for the due state. */
@Composable
fun DuePill(due: DueState) {
    val color = dueColor(due)
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            dueLabel(due),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun movementReasonLabel(r: MovementReason): String = stringResource(
    when (r) {
        MovementReason.PURCHASE -> R.string.mov_purchase
        MovementReason.CONSUMPTION -> R.string.mov_consumption
        MovementReason.CORRECTION -> R.string.mov_correction
        MovementReason.WASTE -> R.string.mov_waste
        MovementReason.RETURN -> R.string.mov_return
        MovementReason.MANUAL -> R.string.mov_manual
    }
)
