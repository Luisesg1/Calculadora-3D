package com.print3d.calculator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Design tokens for the whole app. Every screen/component should pull spacing, radius
 * and elevation from here — no ad-hoc dp values — so the UI stays one consistent system.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** Standard screen edge padding. */
    val screen = 16.dp
    /** Gap between stacked cards/sections in a list. */
    val section = 14.dp
}

/** Corner radii — kept to a small ladder so nothing feels mismatched. */
object Radius {
    val sm = RoundedCornerShape(10.dp)   // chips, small controls
    val md = RoundedCornerShape(14.dp)   // inputs
    val lg = RoundedCornerShape(18.dp)   // cards, tiles
    val xl = RoundedCornerShape(24.dp)   // dialogs, sheets, hero
    val pill = RoundedCornerShape(50)
}

/** Very subtle elevations — Linear/Stripe style, never heavy. */
object Elevation {
    val none = 0.dp
    val card = 1.dp
    val raised = 3.dp
    val overlay = 8.dp
}
