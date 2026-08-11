package com.print3d.calculator.feature.monetization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.print3d.calculator.R
import com.print3d.calculator.ui.theme.IndigoGradientTip
import com.print3d.calculator.ui.theme.layerLines

/**
 * "Hazte Pro" upsell. A single branded gradient card that sells the subscription by listing
 * its benefits — shown in Settings. Collapses to a compact "you're Pro" confirmation once active.
 */
@Composable
fun ProUpsellCard(
    isSubscribed: Boolean,
    monthlyPrice: String,
    annualPrice: String,
    onSubscribe: (basePlanId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var plan by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(com.print3d.calculator.data.billing.BillingRepository.BASE_PLAN_ANNUAL)
    }
    val primary = MaterialTheme.colorScheme.primary
    val gradient = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(lerp(primary, Color.Black, 0.06f), primary, lerp(primary, IndigoGradientTip, 0.55f))
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .background(gradient)
                .layerLines(Color.White)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(if (isSubscribed) R.string.pro_active else R.string.pro_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (!isSubscribed) {
                Spacer(Modifier.height(14.dp))
                Benefit(stringResource(R.string.pro_benefit_noads))
                Benefit(stringResource(R.string.pro_benefit_logo))
                Benefit(stringResource(R.string.pro_benefit_watermark))
                Benefit(stringResource(R.string.pro_benefit_unlimited))
                Benefit(stringResource(R.string.pro_benefit_support))
                Spacer(Modifier.height(16.dp))

                // Plan selector: annual (highlighted, 2 months free) vs monthly.
                PlanOption(
                    selected = plan == com.print3d.calculator.data.billing.BillingRepository.BASE_PLAN_ANNUAL,
                    title = stringResource(R.string.plan_annual),
                    price = stringResource(R.string.per_year, annualPrice),
                    badge = stringResource(R.string.plan_annual_free),
                    accent = primary,
                    onClick = { plan = com.print3d.calculator.data.billing.BillingRepository.BASE_PLAN_ANNUAL }
                )
                Spacer(Modifier.height(8.dp))
                PlanOption(
                    selected = plan == com.print3d.calculator.data.billing.BillingRepository.BASE_PLAN_MONTHLY,
                    title = stringResource(R.string.plan_monthly),
                    price = stringResource(R.string.per_month, monthlyPrice),
                    badge = null,
                    accent = primary,
                    onClick = { plan = com.print3d.calculator.data.billing.BillingRepository.BASE_PLAN_MONTHLY }
                )

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onSubscribe(plan) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pro_cta), fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.pro_active_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(
        Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}

@Composable
private fun PlanOption(
    selected: Boolean,
    title: String,
    price: String,
    badge: String?,
    accent: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) Color.White else Color.White.copy(alpha = 0.12f)
    val fg = if (selected) accent else Color.White
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                null, tint = fg, modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = fg)
                    if (badge != null) {
                        Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = if (selected) 0.14f else 0.0f)) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) accent else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Text(price, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.95f))
        }
    }
}
