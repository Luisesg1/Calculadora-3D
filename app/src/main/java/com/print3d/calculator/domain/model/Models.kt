package com.print3d.calculator.domain.model

import kotlinx.serialization.Serializable

data class Client(
    val id: Long = 0,
    val name: String,
    val rut: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = ""
)

data class Material(
    val id: Long = 0,
    val name: String,
    val brand: String = "",
    val color: String = "",
    val spoolWeightG: Double = 1000.0,
    val spoolPrice: Double = 0.0,
    val diameterMm: Double = 1.75,
    val densityG: Double? = null,
    /** Grams remaining on the spool. Defaults to the full spool for new/legacy materials. */
    val currentWeightG: Double = spoolWeightG,
    /** Low-stock threshold in grams; 0 disables the alert. */
    val minStockG: Double = 0.0,
    val purchaseDate: Long? = null
) {
    /** Price of a single gram of filament. */
    val pricePerGram: Double
        get() = if (spoolWeightG > 0) spoolPrice / spoolWeightG else 0.0

    /** Name with color appended when set, e.g. "PLA+ · Negro" — disambiguates same-name spools. */
    val displayLabel: String
        get() = if (color.isBlank()) name else "$name · $color"

    /** Remaining stock as a fraction of the initial spool weight, clamped to 0..1. */
    val stockPct: Double
        get() = if (spoolWeightG > 0) (currentWeightG / spoolWeightG).coerceIn(0.0, 1.0) else 0.0

    val stockStatus: StockStatus
        get() = when {
            currentWeightG <= 0.0 -> StockStatus.OUT
            minStockG > 0.0 && currentWeightG <= minStockG -> StockStatus.LOW
            else -> StockStatus.OK
        }
}

data class Machine(
    val id: Long = 0,
    val name: String,
    val brand: String = "",
    val model: String = "",
    val price: Double = 0.0,
    val lifespanH: Double = 0.0,
    val powerW: Double = 0.0,
    val hourCost: Double = 0.0,
    val notes: String = ""
) {
    /** Hourly machine cost: explicit rate if set, otherwise straight-line depreciation. */
    val effectiveHourCost: Double
        get() = when {
            hourCost > 0 -> hourCost
            lifespanH > 0 -> price / lifespanH
            else -> 0.0
        }
}

/** A single named extra cost line entered on a quote. */
@Serializable
data class CostLine(
    val key: String,
    val labelResName: String? = null,
    val customLabel: String? = null,
    val amount: Double = 0.0
)

/** Everything the calculator needs to produce a result. Persisted as part of a quote. */
/** One material used in a job: which spool and how many grams of it. */
@Serializable
data class MaterialLine(
    val materialId: Long? = null,
    val grams: Double = 0.0
)

@Serializable
data class QuoteInput(
    val clientName: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val projectName: String = "",
    val notes: String = "",
    val quantity: Int = 1,
    val materialId: Long? = null,
    val machineId: Long? = null,
    val grams: Double = 0.0,
    /** Multi-material lines. Empty on legacy quotes — see [effectiveMaterialLines]. */
    val materialLines: List<MaterialLine> = emptyList(),
    val meters: Double = 0.0,
    val printTimeH: Double = 0.0,
    val postTimeH: Double = 0.0,
    val failurePct: Double = 0.0,
    val wastePct: Double = 0.0,
    val extraCosts: List<CostLine> = emptyList(),
    val marginPct: Double = 40.0,
    val taxPct: Double = 0.0,
    val discountPct: Double = 0.0,
    val surchargePct: Double = 0.0,
    val manualFinalPrice: Double? = null
) {
    /** Material lines to price: the multi-material list, or the legacy single material as one line. */
    val effectiveMaterialLines: List<MaterialLine>
        get() = when {
            materialLines.isNotEmpty() -> materialLines
            materialId != null || grams > 0.0 -> listOf(MaterialLine(materialId, grams))
            else -> emptyList()
        }

    /** Total grams across all materials (falls back to legacy [grams]). */
    val totalGrams: Double
        get() = effectiveMaterialLines.sumOf { it.grams }.let { if (it > 0.0) it else grams }
}

/** Computed, immutable result of a calculation. All values are for the whole job. */
@Serializable
data class QuoteResult(
    val materialCost: Double,
    val electricityCost: Double,
    val machineCost: Double,
    val laborCost: Double,
    val extrasCost: Double,
    val productionCost: Double,
    val profit: Double,
    val subtotal: Double,
    val discountAmount: Double,
    val surchargeAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val quantity: Int
) {
    val perUnit: Double get() = if (quantity > 0) total / quantity else total

    /** Profit as a share of the final price (markup on price), matching the reference calculator. */
    val profitPctOfPrice: Double get() = if (subtotal > 0) profit / subtotal * 100.0 else 0.0
}

data class Quotation(
    val id: Long = 0,
    val number: String,
    val createdAt: Long,
    val input: QuoteInput,
    val result: QuoteResult,
    val currencyCode: String,
    val isFavorite: Boolean = false,
    val status: QuoteStatus = QuoteStatus.DRAFT,
    /** Last time the quote was edited or its status changed. Defaults to createdAt. */
    val updatedAt: Long = createdAt,
    /** Optional expiry date (millis). Only drives the [dueState] badge; never mutates [status]. */
    val dueDate: Long? = null,
    /** True once production consumed material from inventory — guards against double deduction. */
    val stockDeducted: Boolean = false,
    // Lifecycle timestamps (millis), set when the quote first reaches each state.
    val sentAt: Long? = null,
    val viewedAt: Long? = null,
    val acceptedAt: Long? = null,
    val rejectedAt: Long? = null,
    val productionStartedAt: Long? = null,
    val deliveredAt: Long? = null
) {
    /** Vencimiento derivado — "por vencer" a 3 días o menos. Nunca cambia el estado. */
    fun dueState(now: Long = System.currentTimeMillis()): DueState {
        val d = dueDate ?: return DueState.NONE
        val daysLeft = (d - now) / 86_400_000.0
        return when {
            daysLeft < 0 -> DueState.OVERDUE
            daysLeft <= 3 -> DueState.DUE_SOON
            else -> DueState.VALID
        }
    }
}

/** One entry in a material's stock ledger. */
data class MaterialMovement(
    val id: Long = 0,
    val materialId: Long,
    val delta: Double,
    val reason: MovementReason,
    val previousWeightG: Double,
    val newWeightG: Double,
    val timestamp: Long,
    val note: String = "",
    val quotationId: Long? = null
)

/** One status change in a quote's lifecycle — the CRM audit trail. */
data class QuoteEvent(
    val id: Long = 0,
    val quotationId: Long,
    val status: QuoteStatus,
    val timestamp: Long,
    val note: String = ""
)

/** A reusable, named preset of quote inputs — Pro feature (free tier gets one). */
data class QuoteTemplate(
    val id: Long = 0,
    val name: String,
    val input: QuoteInput
)
