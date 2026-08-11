package com.print3d.calculator.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.Quotation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class StatsRange { THIS_MONTH, LAST_3M, THIS_YEAR, ALL }

data class MonthBucket(val label: String, val profit: Double)

data class StatsData(
    val income: Double = 0.0,
    val costs: Double = 0.0,
    val profit: Double = 0.0,
    val quotes: Int = 0,
    val avgTicket: Double = 0.0,
    val months: List<MonthBucket> = emptyList()
)

data class StatsUiState(
    val settings: AppSettings = AppSettings(),
    val range: StatsRange = StatsRange.THIS_MONTH,
    val data: StatsData = StatsData()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    quoteRepo: QuotationRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    private val range = MutableStateFlow(StatsRange.THIS_MONTH)
    fun setRange(r: StatsRange) { range.value = r }

    val state: StateFlow<StatsUiState> =
        combine(quoteRepo.all, settingsRepo.settings, range) { quotes, settings, r ->
            StatsUiState(settings = settings, range = r, data = compute(quotes, r))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    private fun rangeStart(r: StatsRange): Long {
        if (r == StatsRange.ALL) return 0L
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        when (r) {
            StatsRange.THIS_MONTH -> c.set(Calendar.DAY_OF_MONTH, 1)
            StatsRange.LAST_3M -> { c.set(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.MONTH, -2) }
            StatsRange.THIS_YEAR -> { c.set(Calendar.DAY_OF_YEAR, 1) }
            StatsRange.ALL -> {}
        }
        return c.timeInMillis
    }

    private fun compute(all: List<Quotation>, r: StatsRange): StatsData {
        val start = rangeStart(r)
        val quotes = all.filter { it.createdAt >= start }
        if (quotes.isEmpty()) return StatsData()
        val income = quotes.sumOf { it.result.total }
        val costs = quotes.sumOf { it.result.productionCost }
        val profit = income - costs

        val monthFmt = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
        val buckets = quotes.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
            c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH)
        }.toSortedMap().map { (key, list) ->
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, key / 100); set(Calendar.MONTH, key % 100)
            }
            MonthBucket(
                label = monthFmt.format(c.time).replaceFirstChar { it.uppercase() },
                profit = list.sumOf { q -> q.result.total - q.result.productionCost }
            )
        }.takeLast(6)

        return StatsData(
            income = income,
            costs = costs,
            profit = profit,
            quotes = quotes.size,
            avgTicket = income / quotes.size,
            months = buckets
        )
    }
}
