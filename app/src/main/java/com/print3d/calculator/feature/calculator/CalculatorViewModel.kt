package com.print3d.calculator.feature.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.ClientRepository
import com.print3d.calculator.data.repo.MachineRepository
import com.print3d.calculator.data.repo.MaterialRepository
import com.print3d.calculator.data.repo.QuotationRepository
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.calc.CalculationEngine
import com.print3d.calculator.domain.model.Client
import com.print3d.calculator.domain.model.Machine
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.QuoteInput
import com.print3d.calculator.domain.model.QuoteResult
import com.print3d.calculator.domain.model.Quotation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val materialRepo: MaterialRepository,
    private val machineRepo: MachineRepository,
    private val quoteRepo: QuotationRepository,
    private val clientRepo: ClientRepository,
    private val templateRepo: com.print3d.calculator.data.repo.TemplateRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    val materials: StateFlow<List<Material>> = materialRepo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<com.print3d.calculator.domain.model.QuoteTemplate>> = templateRepo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun templateCount(): Int = templateRepo.count()
    suspend fun saveTemplate(name: String, input: QuoteInput): Long =
        templateRepo.save(com.print3d.calculator.domain.model.QuoteTemplate(name = name, input = input))
    fun deleteTemplate(t: com.print3d.calculator.domain.model.QuoteTemplate) =
        viewModelScope.launch { templateRepo.delete(t) }
    val machines: StateFlow<List<Machine>> = machineRepo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    /** Saved clients — power the client name autocomplete and contact auto-fill. */
    val clients: StateFlow<List<Client>> = clientRepo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientSuggestions: StateFlow<List<String>> = clients
        .map { list -> list.map { it.name }.sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun calculate(
        input: QuoteInput,
        materials: List<Material>,
        machine: Machine?,
        electricityRate: Double
    ): QuoteResult {
        val byId = materials.associateBy { it.id }
        return CalculationEngine.calculate(input, { id -> id?.let { byId[it] } }, machine, electricityRate)
    }

    /** Save a material created inline from the calculator; returns its new id. */
    suspend fun saveMaterial(material: Material): Long = materialRepo.save(material)

    /** Save a printer created inline from the calculator; returns its new id. */
    suspend fun saveMachine(machine: Machine): Long = machineRepo.save(machine)

    suspend fun loadQuote(id: Long): Quotation? = quoteRepo.get(id)

    suspend fun nextNumber(): String = quoteRepo.nextNumber()

    /** Number of quotes already saved — used to enforce the free-tier cap. */
    suspend fun savedCount(): Int = quoteRepo.count()

    /** Quotes saved this calendar month — enforces the free monthly cap. */
    suspend fun savedThisMonth(): Int = quoteRepo.countThisMonth()

    suspend fun save(input: QuoteInput, result: QuoteResult, currencyCode: String, existingId: Long): Long {
        val now = System.currentTimeMillis()
        val existing = if (existingId > 0) quoteRepo.get(existingId) else null
        val quote = if (existing != null) {
            // Edit: preserve creation date, status and lifecycle; only refresh content + updatedAt.
            existing.copy(input = input, result = result, currencyCode = currencyCode, updatedAt = now)
        } else {
            Quotation(
                id = 0,
                number = quoteRepo.nextNumber(),
                createdAt = now,
                input = input,
                result = result,
                currencyCode = currencyCode,
                updatedAt = now
            )
        }
        val id = quoteRepo.save(quote)
        if (existing == null) quoteRepo.logEvent(id, com.print3d.calculator.domain.model.QuoteStatus.DRAFT)
        // Keep the client directory in sync with quoted clients.
        clientRepo.ensureExists(input.clientName, input.clientPhone, input.clientEmail)
        return id
    }
}
