package com.print3d.calculator.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.domain.model.Quotation
import com.print3d.calculator.domain.model.QuoteStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: QuotationRepository
) : ViewModel() {

    val query = MutableStateFlow("")

    val quotes: StateFlow<List<Quotation>> = query
        .flatMapLatest { q -> if (q.isBlank()) repo.all else repo.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { query.value = q }
    fun toggleFavorite(q: Quotation) = viewModelScope.launch {
        repo.update(q.copy(isFavorite = !q.isFavorite))
    }
    fun setStatus(q: Quotation, status: QuoteStatus) = viewModelScope.launch {
        repo.update(q.copy(status = status))
    }
    fun delete(q: Quotation) = viewModelScope.launch { repo.delete(q) }
    fun duplicate(q: Quotation) = viewModelScope.launch {
        repo.save(q.copy(id = 0, number = repo.nextNumber(), createdAt = System.currentTimeMillis(), isFavorite = false, status = QuoteStatus.DRAFT))
    }
}
