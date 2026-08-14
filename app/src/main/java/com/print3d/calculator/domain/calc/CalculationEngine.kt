package com.print3d.calculator.domain.calc

import com.print3d.calculator.domain.model.Machine
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.QuoteInput
import com.print3d.calculator.domain.model.QuoteResult

/**
 * Pure, side-effect-free pricing engine. Given the job input and the selected
 * material/machine plus the electricity rate, it produces a full cost breakdown.
 *
 * All amounts represent the whole job (see [QuoteResult.perUnit] for per-piece).
 */
object CalculationEngine {

    fun calculate(
        input: QuoteInput,
        materialLookup: (Long?) -> Material?,
        machine: Machine?,
        electricityRatePerKwh: Double
    ): QuoteResult {
        // Clamp every percentage to a sane range so absurd inputs (or old saved quotes)
        // can't produce runaway totals. Failure/waste/tax/discount are bounded by 100%;
        // margin/surcharge get a generous 10000% ceiling.
        val failurePct = input.failurePct.coerceIn(0.0, 100.0)
        val wastePct = input.wastePct.coerceIn(0.0, 100.0)
        val marginPct = input.marginPct.coerceIn(0.0, 10000.0)
        val surchargePct = input.surchargePct.coerceIn(0.0, 10000.0)
        val discountPct = input.discountPct.coerceIn(0.0, 100.0)
        val taxPct = input.taxPct.coerceIn(0.0, 100.0)

        val failureMult = 1.0 + (failurePct / 100.0)
        val wasteMult = 1.0 + (wastePct / 100.0)

        // Sum each material line: grams × waste × its price per gram.
        val materialBase = input.effectiveMaterialLines.sumOf { line ->
            line.grams * wasteMult * (materialLookup(line.materialId)?.pricePerGram ?: 0.0)
        }

        val electricityBase =
            (machine?.powerW ?: 0.0) / 1000.0 * input.printTimeH * electricityRatePerKwh

        val machineBase = (machine?.effectiveHourCost ?: 0.0) * input.printTimeH

        // Failed prints re-consume material, power and machine time.
        val materialCost = materialBase * failureMult
        val electricityCost = electricityBase * failureMult
        val machineCost = machineBase * failureMult

        val laborCost = input.extraCosts
            .filter { it.key == CostKeys.LABOR }
            .sumOf { it.amount }
        val extrasCost = input.extraCosts
            .filter { it.key != CostKeys.LABOR }
            .sumOf { it.amount }

        val productionCost =
            materialCost + electricityCost + machineCost + laborCost + extrasCost

        val profit = productionCost * (marginPct / 100.0)
        val baseSubtotal = productionCost + profit

        val surchargeAmount = baseSubtotal * (surchargePct / 100.0)
        val discountAmount = (baseSubtotal + surchargeAmount) * (discountPct / 100.0)
        val taxable = baseSubtotal + surchargeAmount - discountAmount
        val taxAmount = taxable * (taxPct / 100.0)
        val computedTotal = taxable + taxAmount

        val total = input.manualFinalPrice ?: computedTotal

        return QuoteResult(
            materialCost = materialCost,
            electricityCost = electricityCost,
            machineCost = machineCost,
            laborCost = laborCost,
            extrasCost = extrasCost,
            productionCost = productionCost,
            profit = profit,
            subtotal = baseSubtotal,
            discountAmount = discountAmount,
            surchargeAmount = surchargeAmount,
            taxAmount = taxAmount,
            total = total,
            quantity = input.quantity.coerceAtLeast(1)
        )
    }
}
