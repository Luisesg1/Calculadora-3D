package com.print3d.calculator.feature.quotedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.ClientRepository
import com.print3d.calculator.data.repo.MaterialRepository
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.Client
import com.print3d.calculator.domain.model.QuoteEvent
import com.print3d.calculator.domain.model.QuoteStatus
import com.print3d.calculator.domain.model.Quotation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: QuotationRepository,
    private val clientRepo: ClientRepository,
    private val materialRepo: MaterialRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    val quoteId: Long = savedState.get<Long>("quoteId") ?: -1L

    private val _quote = MutableStateFlow<Quotation?>(null)
    val quote: StateFlow<Quotation?> = _quote

    /** The saved client matching this quote's name, for the contact card + profile link. */
    private val _client = MutableStateFlow<Client?>(null)
    val client: StateFlow<Client?> = _client

    /** First material's display label, used in the exported PDF summary. */
    private val _materialLabel = MutableStateFlow<String?>(null)
    val materialLabel: StateFlow<String?> = _materialLabel

    val events: StateFlow<List<QuoteEvent>> = repo.events(quoteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    init { reload() }

    private fun reload() = viewModelScope.launch {
        val q = repo.get(quoteId)
        _quote.value = q
        if (q != null) {
            _client.value = clientRepo.getByName(q.input.clientName)
            val firstId = q.input.effectiveMaterialLines.firstOrNull()?.materialId
            _materialLabel.value = firstId?.let { materialRepo.get(it)?.displayLabel }
        }
    }

    fun setStatus(status: QuoteStatus) = viewModelScope.launch {
        val q = _quote.value ?: return@launch
        _quote.value = repo.updateStatus(q, status)
    }

    fun setDueDate(dueDate: Long?) = viewModelScope.launch {
        val q = _quote.value ?: return@launch
        val updated = q.copy(dueDate = dueDate, updatedAt = System.currentTimeMillis())
        repo.update(updated)
        _quote.value = updated
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        _quote.value?.let { repo.delete(it) }
        onDone()
    }
}
