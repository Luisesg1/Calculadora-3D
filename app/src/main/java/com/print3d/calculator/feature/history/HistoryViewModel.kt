package com.print3d.calculator.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Time windows for the date filter. */
enum class DateFilter { ALL, THIS_MONTH, LAST_3M, THIS_YEAR }

data class QuoteFilters(
    val query: String = "",
    val status: QuoteStatus? = null,
    val client: String? = null,
    val date: DateFilter = DateFilter.ALL,
    val sortDesc: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: QuotationRepository
) : ViewModel() {

    private val filters = MutableStateFlow(QuoteFilters())
    val filtersState: StateFlow<QuoteFilters> = filters

    /** All quotes (unfiltered) — used to populate the client filter and the Kanban board. */
    val all: StateFlow<List<Quotation>> = repo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Distinct client names present in saved quotes, for the client filter. */
    val clientNames: StateFlow<List<String>> = repo.all
        .map { list -> list.mapNotNull { it.input.clientName.takeIf { n -> n.isNotBlank() } }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quotes: StateFlow<List<Quotation>> =
        combine(repo.all, filters) { list, f -> applyFilters(list, f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applyFilters(list: List<Quotation>, f: QuoteFilters): List<Quotation> {
        val since = dateStart(f.date)
        val q = f.query.trim().lowercase()
        return list.asSequence()
            .filter { it.createdAt >= since }
            .filter { f.status == null || it.status == f.status }
            .filter { f.client == null || it.input.clientName.equals(f.client, ignoreCase = true) }
            .filter {
                q.isBlank() ||
                    it.input.projectName.lowercase().contains(q) ||
                    it.input.clientName.lowercase().contains(q) ||
                    it.number.lowercase().contains(q)
            }
            .sortedWith(
                compareByDescending<Quotation> { it.isFavorite }
                    .thenComparator { a, b ->
                        if (f.sortDesc) b.createdAt.compareTo(a.createdAt)
                        else a.createdAt.compareTo(b.createdAt)
                    }
            )
            .toList()
    }

    private fun dateStart(f: DateFilter): Long {
        if (f == DateFilter.ALL) return 0L
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        when (f) {
            DateFilter.THIS_MONTH -> c.set(Calendar.DAY_OF_MONTH, 1)
            DateFilter.LAST_3M -> { c.set(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.MONTH, -2) }
            DateFilter.THIS_YEAR -> c.set(Calendar.DAY_OF_YEAR, 1)
            DateFilter.ALL -> {}
        }
        return c.timeInMillis
    }

    fun setQuery(q: String) { filters.value = filters.value.copy(query = q) }
    fun setStatusFilter(s: QuoteStatus?) { filters.value = filters.value.copy(status = s) }
    fun setClientFilter(c: String?) { filters.value = filters.value.copy(client = c) }
    fun setDateFilter(d: DateFilter) { filters.value = filters.value.copy(date = d) }
    fun toggleSort() { filters.value = filters.value.copy(sortDesc = !filters.value.sortDesc) }

    fun toggleFavorite(q: Quotation) = viewModelScope.launch {
        repo.update(q.copy(isFavorite = !q.isFavorite))
    }
    fun setStatus(q: Quotation, status: QuoteStatus) = viewModelScope.launch {
        repo.updateStatus(q, status)
    }
    fun delete(q: Quotation) = viewModelScope.launch { repo.delete(q) }
    fun duplicate(q: Quotation) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val copy = q.copy(
            id = 0, number = repo.nextNumber(), createdAt = now, updatedAt = now,
            isFavorite = false, status = QuoteStatus.DRAFT, stockDeducted = false, dueDate = null,
            sentAt = null, viewedAt = null, acceptedAt = null, rejectedAt = null,
            productionStartedAt = null, deliveredAt = null
        )
        val id = repo.save(copy)
        repo.logEvent(id, QuoteStatus.DRAFT)
    }
}
