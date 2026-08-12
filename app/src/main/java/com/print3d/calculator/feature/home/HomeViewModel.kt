package com.print3d.calculator.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.ClientRepository
import com.print3d.calculator.data.repo.MaterialRepository
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteStatus
import com.print3d.calculator.domain.model.StockStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class HomeStats(
    val quotesCount: Int = 0,
    val quotesThisMonth: Int = 0,
    val incomeTotal: Double = 0.0,
    val materialKg: Double = 0.0,
    val clientsCount: Int = 0,
    // Business dashboard.
    val pendingQuotes: Int = 0,
    val acceptedQuotes: Int = 0,
    val inProduction: Int = 0,
    val lowStockCount: Int = 0,
    val incomeThisMonth: Double = 0.0,
    val profitThisMonth: Double = 0.0
)

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val stats: HomeStats = HomeStats(),
    val recent: List<Quotation> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    quoteRepo: QuotationRepository,
    clientRepo: ClientRepository,
    materialRepo: MaterialRepository,
    settingsRepo: SettingsRepository,
    private val guestQuota: com.print3d.calculator.data.quota.GuestQuotaManager
) : ViewModel() {

    /** Spend one guest quote attempt (only called when not signed in). */
    suspend fun tryConsumeGuestQuote(): Boolean = guestQuota.tryConsume()

    val state: StateFlow<HomeUiState> =
        combine(quoteRepo.all, clientRepo.all, materialRepo.all, settingsRepo.settings) { quotes, clients, materials, settings ->
            HomeUiState(
                settings = settings,
                stats = computeStats(quotes, clients.size, materials),
                recent = quotes.sortedByDescending { it.createdAt }.take(5)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun computeStats(quotes: List<Quotation>, clientsCount: Int, materials: List<Material>): HomeStats {
        val lowStock = materials.count { it.stockStatus != StockStatus.OK }
        if (quotes.isEmpty()) return HomeStats(clientsCount = clientsCount, lowStockCount = lowStock)
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH)
        val year = now.get(Calendar.YEAR)
        val monthQuotes = quotes.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
            c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
        }
        val grams = quotes.sumOf { it.input.totalGrams }
        return HomeStats(
            quotesCount = quotes.size,
            quotesThisMonth = monthQuotes.size,
            incomeTotal = quotes.sumOf { it.result.total },
            materialKg = grams / 1000.0,
            clientsCount = clientsCount,
            pendingQuotes = quotes.count { it.status == QuoteStatus.DRAFT || it.status == QuoteStatus.SENT || it.status == QuoteStatus.VIEWED },
            acceptedQuotes = quotes.count { it.status == QuoteStatus.ACCEPTED },
            inProduction = quotes.count { it.status == QuoteStatus.IN_PRODUCTION },
            lowStockCount = lowStock,
            incomeThisMonth = monthQuotes.sumOf { it.result.total },
            profitThisMonth = monthQuotes.sumOf { it.result.profit }
        )
    }
}
