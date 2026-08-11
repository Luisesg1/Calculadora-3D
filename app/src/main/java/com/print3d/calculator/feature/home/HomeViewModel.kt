package com.print3d.calculator.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.ClientRepository
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.Quotation
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
    val clientsCount: Int = 0
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
    settingsRepo: SettingsRepository,
    private val guestQuota: com.print3d.calculator.data.quota.GuestQuotaManager
) : ViewModel() {

    /** Spend one guest quote attempt (only called when not signed in). */
    suspend fun tryConsumeGuestQuote(): Boolean = guestQuota.tryConsume()

    val state: StateFlow<HomeUiState> =
        combine(quoteRepo.all, clientRepo.all, settingsRepo.settings) { quotes, clients, settings ->
            HomeUiState(
                settings = settings,
                stats = computeStats(quotes, clients.size),
                recent = quotes.sortedByDescending { it.createdAt }.take(5)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun computeStats(quotes: List<Quotation>, clientsCount: Int): HomeStats {
        if (quotes.isEmpty()) return HomeStats(clientsCount = clientsCount)
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH)
        val year = now.get(Calendar.YEAR)
        val thisMonth = quotes.count {
            val c = Calendar.getInstance().apply { timeInMillis = it.createdAt }
            c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
        }
        val grams = quotes.sumOf { it.input.grams }
        return HomeStats(
            quotesCount = quotes.size,
            quotesThisMonth = thisMonth,
            incomeTotal = quotes.sumOf { it.result.total },
            materialKg = grams / 1000.0,
            clientsCount = clientsCount
        )
    }
}
