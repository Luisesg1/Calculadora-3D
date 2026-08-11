package com.print3d.calculator.domain.model

import kotlinx.serialization.Serializable

data class Client(
    val id: Long = 0,
    val name: String,
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
    val densityG: Double? = null
) {
    /** Price of a single gram of filament. */
    val pricePerGram: Double
        get() = if (spoolWeightG > 0) spoolPrice / spoolWeightG else 0.0

    /** Name with color appended when set, e.g. "PLA+ · Negro" — disambiguates same-name spools. */
    val displayLabel: String
        get() = if (color.isBlank()) name else "$name · $color"
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
    val status: QuoteStatus = QuoteStatus.DRAFT
)

/** A reusable, named preset of quote inputs — Pro feature (free tier gets one). */
data class QuoteTemplate(
    val id: Long = 0,
    val name: String,
    val input: QuoteInput
)
