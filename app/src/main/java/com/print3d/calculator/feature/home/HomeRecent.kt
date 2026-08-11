package com.print3d.calculator.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.core.util.DateFormatter
import com.print3d.calculator.core.util.RelativeTime
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.Quotation

/* ---------- Featured last quote ---------- */

@Composable
fun FeaturedLastQuote(
    quote: Quotation,
    dateFormat: String,
    lastQuoteLabel: String,
    clientLabel: String,
    totalLabel: String,
    viewLabel: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val currency = AppCurrency.fromCode(quote.currencyCode)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = com.print3d.calculator.ui.theme.Elevation.card,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    lastQuoteLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    RelativeTime.format(context, quote.createdAt, dateFormat),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                quote.input.projectName.ifBlank { quote.number },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$clientLabel: ${quote.input.clientName.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        totalLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        CurrencyFormatter.format(quote.result.total, currency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                FilledTonalButton(onClick = onClick) {
                    Text(viewLabel)
                    Spacer(Modifier.size(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/* ---------- Recent list (grouped, subtle dividers) ---------- */

@Composable
fun RecentActivityList(
    quotes: List<Quotation>,
    dateFormat: String,
    statusFavorite: String,
    statusSaved: String,
    onOpen: (Long) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = com.print3d.calculator.ui.theme.CardStyle.elevation,
        border = com.print3d.calculator.ui.theme.CardStyle.border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            quotes.forEachIndexed { index, q ->
                RecentRow(q, dateFormat, statusFavorite, statusSaved) { onOpen(q.id) }
                if (index < quotes.lastIndex) {
                    HorizontalDivider(
                        Modifier.padding(start = 72.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentRow(
    quote: Quotation,
    dateFormat: String,
    statusFavorite: String,
    statusSaved: String,
    onClick: () -> Unit
) {
    val currency = AppCurrency.fromCode(quote.currencyCode)
    Row(
        Modifier
            .fillMaxWidth()
            .let { it }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                quote.input.projectName.ifBlank { quote.number },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                "${quote.input.clientName.ifBlank { "—" }} · ${DateFormatter.format(quote.createdAt, dateFormat)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                CurrencyFormatter.format(quote.result.total, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            StatusPill(if (quote.isFavorite) statusFavorite else statusSaved, quote.isFavorite)
        }
    }
}

@Composable
private fun StatusPill(text: String, favorite: Boolean) {
    val bg = if (favorite) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (favorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.background(bg, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (favorite) {
            Icon(Icons.Rounded.Star, null, tint = fg, modifier = Modifier.size(12.dp))
            Spacer(Modifier.size(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/* ---------- Empty state ---------- */

@Composable
fun RecentEmptyState(message: String, actionLabel: String, onCreate: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = com.print3d.calculator.ui.theme.CardStyle.elevation,
        border = com.print3d.calculator.ui.theme.CardStyle.border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreate) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(actionLabel)
            }
        }
    }
}
