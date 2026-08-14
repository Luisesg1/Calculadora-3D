package com.print3d.calculator.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.MaterialMovementRepository
import com.print3d.calculator.data.repo.MaterialRepository
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.DueState
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.MaterialMovement
import com.print3d.calculator.domain.model.MovementReason
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class StatsRange { THIS_MONTH, LAST_3M, THIS_YEAR, ALL }

data class MonthBucket(
    val label: String,
    val profit: Double,
    val income: Double = 0.0,
    val costs: Double = 0.0
)

/** A client's aggregate footprint within the selected range. */
data class ClientStat(val name: String, val revenue: Double, val quotes: Int)

data class StatsData(
    val income: Double = 0.0,
    val costs: Double = 0.0,
    val profit: Double = 0.0,
    val quotes: Int = 0,
    val avgTicket: Double = 0.0,
    val months: List<MonthBucket> = emptyList(),
    val topClients: List<ClientStat> = emptyList(),
    // CRM metrics.
    val created: Int = 0,
    val accepted: Int = 0,
    val rejected: Int = 0,
    val pending: Int = 0,
    val inProduction: Int = 0,
    val overdue: Int = 0,
    val conversionRate: Double = 0.0,
    val salesFromQuotes: Double = 0.0,
    val profitFromQuotes: Double = 0.0,
    // Inventory metrics.
    val mostUsedMaterial: String? = null,
    val topConsumptionMaterial: String? = null,
    val materialsConsumedCost: Double = 0.0
)

data class StatsUiState(
    val settings: AppSettings = AppSettings(),
    val range: StatsRange = StatsRange.THIS_MONTH,
    val data: StatsData = StatsData()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    quoteRepo: QuotationRepository,
    materialRepo: MaterialRepository,
    movementRepo: MaterialMovementRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    private val range = MutableStateFlow(StatsRange.THIS_MONTH)
    fun setRange(r: StatsRange) { range.value = r }

    val state: StateFlow<StatsUiState> =
        combine(quoteRepo.all, materialRepo.all, movementRepo.all, settingsRepo.settings, range) { quotes, materials, movements, settings, r ->
            StatsUiState(settings = settings, range = r, data = compute(quotes, r, materials, movements))
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

    private fun compute(all: List<Quotation>, r: StatsRange, materials: List<Material>, movements: List<MaterialMovement>): StatsData {
        val start = rangeStart(r)
        val quotes = all.filter { it.createdAt >= start }
        val byId = materials.associateBy { it.id }

        // Inventory consumption in range (from the movement ledger).
        val consumption = movements.filter { it.reason == MovementReason.CONSUMPTION && it.timestamp >= start }
        val gramsByMaterial = consumption.groupBy { it.materialId }.mapValues { (_, list) -> list.sumOf { -it.delta } }
        val topConsumptionId = gramsByMaterial.maxByOrNull { it.value }?.key
        val consumedCost = gramsByMaterial.entries.sumOf { (id, g) -> g * (byId[id]?.pricePerGram ?: 0.0) }

        // Most-used material by how many quotes reference it.
        val usageCount = quotes.flatMap { it.input.effectiveMaterialLines.mapNotNull { l -> l.materialId } }
            .groupingBy { it }.eachCount()
        val mostUsedId = usageCount.maxByOrNull { it.value }?.key

        if (quotes.isEmpty()) {
            return StatsData(
                mostUsedMaterial = mostUsedId?.let { byId[it]?.displayLabel },
                topConsumptionMaterial = topConsumptionId?.let { byId[it]?.displayLabel },
                materialsConsumedCost = consumedCost
            )
        }

        val income = quotes.sumOf { it.result.total }
        val costs = quotes.sumOf { it.result.productionCost }
        val profit = income - costs

        val won = quotes.filter {
            it.status == QuoteStatus.ACCEPTED || it.status == QuoteStatus.IN_PRODUCTION || it.status == QuoteStatus.DELIVERED
        }
        val accepted = won.size
        val rejected = quotes.count { it.status == QuoteStatus.REJECTED }
        val pending = quotes.count { it.status == QuoteStatus.DRAFT || it.status == QuoteStatus.SENT || it.status == QuoteStatus.VIEWED }
        val inProduction = quotes.count { it.status == QuoteStatus.IN_PRODUCTION }
        val overdue = quotes.count { !it.status.isTerminal && it.dueState() == DueState.OVERDUE }
        val conversion = if (quotes.isNotEmpty()) accepted.toDouble() / quotes.size * 100.0 else 0.0

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
                profit = list.sumOf { q -> q.result.total - q.result.productionCost },
                income = list.sumOf { q -> q.result.total },
                costs = list.sumOf { q -> q.result.productionCost }
            )
        }.takeLast(6)

        // Top clients by revenue in range (named clients only; walk-ins with blank names are skipped).
        val topClients = quotes
            .filter { it.input.clientName.isNotBlank() }
            .groupBy { it.input.clientName.trim() }
            .map { (name, list) -> ClientStat(name, list.sumOf { it.result.total }, list.size) }
            .sortedByDescending { it.revenue }
            .take(5)

        return StatsData(
            income = income,
            costs = costs,
            profit = profit,
            quotes = quotes.size,
            avgTicket = income / quotes.size,
            months = buckets,
            topClients = topClients,
            created = quotes.size,
            accepted = accepted,
            rejected = rejected,
            pending = pending,
            inProduction = inProduction,
            overdue = overdue,
            conversionRate = conversion,
            salesFromQuotes = won.sumOf { it.result.total },
            profitFromQuotes = won.sumOf { it.result.profit },
            mostUsedMaterial = mostUsedId?.let { byId[it]?.displayLabel },
            topConsumptionMaterial = topConsumptionId?.let { byId[it]?.displayLabel },
            materialsConsumedCost = consumedCost
        )
    }
}
