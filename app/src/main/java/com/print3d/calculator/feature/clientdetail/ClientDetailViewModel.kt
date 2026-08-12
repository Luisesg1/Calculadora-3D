package com.print3d.calculator.feature.clientdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.ClientRepository
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.domain.model.Client
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientStats(
    val totalBought: Double = 0.0,
    val quotesCount: Int = 0,
    val accepted: Int = 0,
    val rejected: Int = 0
)

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val clientRepo: ClientRepository,
    quoteRepo: QuotationRepository
) : ViewModel() {

    val clientId: Long = savedState.get<Long>("clientId") ?: -1L

    private val _client = MutableStateFlow<Client?>(null)
    val client: StateFlow<Client?> = _client

    /** This client's quotes, matched by name (quotes link to clients by name). */
    val quotes: StateFlow<List<Quotation>> =
        combine(quoteRepo.all, _client) { all, c ->
            if (c == null) emptyList()
            else all.filter { it.input.clientName.equals(c.name, ignoreCase = true) }
                .sortedByDescending { it.createdAt }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ClientStats> = quotes
        .combine(MutableStateFlow(Unit)) { list, _ -> compute(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientStats())

    init {
        viewModelScope.launch { _client.value = clientRepo.get(clientId) }
    }

    private fun compute(list: List<Quotation>): ClientStats {
        // "Bought" counts revenue from won jobs (accepted, in production, delivered).
        val won = list.filter {
            it.status == QuoteStatus.ACCEPTED || it.status == QuoteStatus.IN_PRODUCTION || it.status == QuoteStatus.DELIVERED
        }
        return ClientStats(
            totalBought = won.sumOf { it.result.total },
            quotesCount = list.size,
            accepted = list.count { it.status == QuoteStatus.ACCEPTED || it.status == QuoteStatus.IN_PRODUCTION || it.status == QuoteStatus.DELIVERED },
            rejected = list.count { it.status == QuoteStatus.REJECTED }
        )
    }
}
