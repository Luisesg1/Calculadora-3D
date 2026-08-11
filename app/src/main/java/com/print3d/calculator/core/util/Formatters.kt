package com.print3d.calculator.core.util

import com.print3d.calculator.domain.model.AppCurrency
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, currency: AppCurrency): String {
        val locale = Locale.forLanguageTag(currency.localeTag)
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = currency.decimals
            maximumFractionDigits = currency.decimals
        }
        return "${currency.symbol}${nf.format(amount)}"
    }
}

object RelativeTime {
    /** Human, localized "3 h ago" style label. Falls back to date for anything older than a week. */
    fun format(context: android.content.Context, epochMillis: Long, dateFormat: String): String {
        val diff = System.currentTimeMillis() - epochMillis
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> context.getString(com.print3d.calculator.R.string.time_just_now)
            minutes < 60 -> context.getString(com.print3d.calculator.R.string.time_minutes, minutes.toInt())
            hours < 24 -> context.getString(com.print3d.calculator.R.string.time_hours, hours.toInt())
            days < 7 -> context.getString(com.print3d.calculator.R.string.time_days, days.toInt())
            else -> DateFormatter.format(epochMillis, dateFormat)
        }
    }
}

object DateFormatter {
    fun format(epochMillis: Long, pattern: String, localeTag: String = "es"): String =
        runCatching {
            SimpleDateFormat(pattern, Locale.forLanguageTag(localeTag)).format(Date(epochMillis))
        }.getOrElse { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochMillis)) }
}
