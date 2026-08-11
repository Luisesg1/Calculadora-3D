package com.print3d.calculator.feature.calculator

import com.print3d.calculator.domain.calc.CostKeys
import com.print3d.calculator.domain.model.CostLine
import com.print3d.calculator.domain.model.MaterialLine
import com.print3d.calculator.domain.model.QuoteInput

/** One editable material row in the form: chosen spool + grams as text. */
data class MatLineForm(val materialId: Long? = null, val grams: String = "")

/** UI-facing mutable form. Strings keep text fields clean; converted to [QuoteInput] on demand. */
data class CalcForm(
    val clientName: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val projectName: String = "",
    val notes: String = "",
    val quantity: String = "1",
    val materialLines: List<MatLineForm> = listOf(MatLineForm()),
    val machineId: Long? = null,
    val meters: String = "",
    val printTimeH: String = "",
    val postTimeH: String = "",
    val failurePct: String = "0",
    val wastePct: String = "5",
    val builtInCosts: Map<String, String> = emptyMap(),
    val customCosts: List<CostLine> = emptyList(),
    val marginPct: String = "50",
    val taxPct: String = "0",
    val discountPct: String = "0",
    val surchargePct: String = "0",
    val manualFinalPrice: String = ""
) {
    fun toInput(): QuoteInput {
        val builtIn = CostKeys.BUILT_IN.mapNotNull { (key, res) ->
            val amount = builtInCosts[key]?.toDoubleOrNull() ?: 0.0
            if (amount != 0.0) CostLine(key = key, labelResName = res, amount = amount) else null
        }
        val lines = materialLines
            .map { MaterialLine(it.materialId, it.grams.toDoubleOrNull() ?: 0.0) }
            .filter { it.materialId != null || it.grams > 0.0 }
        return QuoteInput(
            clientName = clientName.trim(),
            clientPhone = clientPhone.trim(),
            clientEmail = clientEmail.trim(),
            projectName = projectName.trim(),
            notes = notes.trim(),
            quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            // Legacy single fields mirror the first line + total grams (keeps stats/back-compat working).
            materialId = lines.firstOrNull()?.materialId,
            machineId = machineId,
            grams = lines.sumOf { it.grams },
            materialLines = lines,
            meters = meters.toDoubleOrNull() ?: 0.0,
            printTimeH = printTimeH.toDoubleOrNull() ?: 0.0,
            postTimeH = postTimeH.toDoubleOrNull() ?: 0.0,
            failurePct = failurePct.toDoubleOrNull() ?: 0.0,
            wastePct = wastePct.toDoubleOrNull() ?: 0.0,
            extraCosts = builtIn + customCosts,
            marginPct = marginPct.toDoubleOrNull() ?: 0.0,
            taxPct = taxPct.toDoubleOrNull() ?: 0.0,
            discountPct = discountPct.toDoubleOrNull() ?: 0.0,
            surchargePct = surchargePct.toDoubleOrNull() ?: 0.0,
            manualFinalPrice = manualFinalPrice.toDoubleOrNull()
        )
    }

    companion object {
        fun from(input: QuoteInput): CalcForm = CalcForm(
            clientName = input.clientName,
            clientPhone = input.clientPhone,
            clientEmail = input.clientEmail,
            projectName = input.projectName,
            notes = input.notes,
            quantity = input.quantity.toString(),
            materialLines = input.effectiveMaterialLines
                .map { MatLineForm(it.materialId, it.grams.takeIf { g -> g != 0.0 }?.toString() ?: "") }
                .ifEmpty { listOf(MatLineForm()) },
            machineId = input.machineId,
            meters = input.meters.takeIf { it != 0.0 }?.toString() ?: "",
            printTimeH = input.printTimeH.takeIf { it != 0.0 }?.toString() ?: "",
            postTimeH = input.postTimeH.takeIf { it != 0.0 }?.toString() ?: "",
            failurePct = input.failurePct.toString(),
            wastePct = input.wastePct.toString(),
            builtInCosts = input.extraCosts
                .filter { line -> CostKeys.BUILT_IN.any { it.first == line.key } }
                .associate { it.key to it.amount.toString() },
            customCosts = input.extraCosts.filter { line ->
                CostKeys.BUILT_IN.none { it.first == line.key }
            },
            marginPct = input.marginPct.toString(),
            taxPct = input.taxPct.toString(),
            discountPct = input.discountPct.toString(),
            surchargePct = input.surchargePct.toString(),
            manualFinalPrice = input.manualFinalPrice?.toString() ?: ""
        )
    }
}
