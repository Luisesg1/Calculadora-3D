package com.print3d.calculator.feature.home

import com.print3d.calculator.ui.theme.IndigoGradientTip
import com.print3d.calculator.ui.theme.layerLines
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.print3d.calculator.core.util.CurrencyFormatter
import com.print3d.calculator.domain.model.AppCurrency

/* ---------- Animated counter ---------- */

/** Animates a value from 0 to [target] on first appearance, and between values thereafter. */
@Composable
private fun animatedValue(target: Float): Float {
    var start by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { start = true }
    val v by animateFloatAsState(
        targetValue = if (start) target else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "counter"
    )
    return v
}

/* ---------- Header ---------- */

@Composable
fun HomeHeader(greeting: String, subtitle: String) {
    Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
        Text(
            greeting,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ---------- Stats dashboard (numbers are the focus) ---------- */

@Composable
fun StatsSummaryCard(
    stats: HomeStats,
    currency: AppCurrency,
    labelQuotes: String,
    labelIncome: String,
    labelMaterial: String,
    labelClients: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = com.print3d.calculator.ui.theme.CardStyle.elevation,
        border = com.print3d.calculator.ui.theme.CardStyle.border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth()) {
                StatCell(Modifier.weight(1f), Icons.Rounded.ReceiptLong, animatedValue(stats.quotesCount.toFloat()).toInt().toString(), labelQuotes)
                Spacer(Modifier.size(12.dp))
                StatCell(Modifier.weight(1f), Icons.Rounded.Payments, CurrencyFormatter.format(animatedValue(stats.incomeTotal.toFloat()).toDouble(), currency), labelIncome)
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth()) {
                StatCell(Modifier.weight(1f), Icons.Rounded.Inventory2, formatKg(animatedValue(stats.materialKg.toFloat()).toDouble()), labelMaterial)
                Spacer(Modifier.size(12.dp))
                StatCell(Modifier.weight(1f), Icons.Rounded.People, animatedValue(stats.clientsCount.toFloat()).toInt().toString(), labelClients)
            }
        }
    }
}

@Composable
private fun StatCell(modifier: Modifier, icon: ImageVector, value: String, label: String) {
    Column(modifier) {
        Box(
            Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun formatKg(kg: Double): String =
    if (kg >= 1.0) String.format("%.1f kg", kg) else String.format("%.0f g", kg * 1000)

/* ---------- Primary CTA ---------- */

@Composable
fun PrimaryQuoteCard(title: String, desc: String, actionLabel: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(), label = "primaryScale")
    val primary = MaterialTheme.colorScheme.primary
    val gradient = Brush.linearGradient(
        listOf(
            lerp(primary, Color.Black, 0.06f),
            primary,
            lerp(primary, IndigoGradientTip, 0.55f)
        )
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().scale(scale)
    ) {
        Row(
            Modifier
                .background(gradient)
                .layerLines(Color.White)
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(56.dp).background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(3.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.88f))
            }
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                actionLabel,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* ---------- Secondary tile (compact, premium) ---------- */

@Composable
fun SecondaryTile(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(), label = "tileScale")
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = com.print3d.calculator.ui.theme.CardStyle.elevation,
        border = com.print3d.calculator.ui.theme.CardStyle.border,
        // Fixed height so every management chip is identical regardless of subtitle length.
        modifier = modifier.scale(scale).height(132.dp)
    ) {
        Column(Modifier.padding(14.dp).fillMaxSize()) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(11.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(1.dp))
            Text(
                desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
