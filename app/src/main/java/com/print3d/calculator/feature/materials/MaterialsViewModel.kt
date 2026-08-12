package com.print3d.calculator.feature.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.InventoryManager
import com.print3d.calculator.data.repo.MaterialMovementRepository
import com.print3d.calculator.data.repo.MaterialRepository
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.Material
import com.print3d.calculator.domain.model.MaterialMovement
import com.print3d.calculator.domain.model.MovementReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialsViewModel @Inject constructor(
    private val repo: MaterialRepository,
    private val inventory: InventoryManager,
    private val movementRepo: MaterialMovementRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    val materials: StateFlow<List<Material>> = repo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Materials at or below their minimum (or out) — powers the low-stock section + alerts. */
    val lowStock: StateFlow<List<Material>> = repo.all
        .map { list -> list.filter { it.stockStatus != com.print3d.calculator.domain.model.StockStatus.OK } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currency: StateFlow<AppCurrency> = settingsRepo.settings
        .map { it.currency }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppCurrency.CLP)

    fun movements(materialId: Long): Flow<List<MaterialMovement>> = movementRepo.forMaterial(materialId)

    fun save(material: Material) = viewModelScope.launch { repo.save(material) }
    fun delete(material: Material) = viewModelScope.launch { repo.delete(material) }
    fun duplicate(material: Material) = viewModelScope.launch {
        repo.save(material.copy(id = 0, name = material.name + " (copia)"))
    }

    /** Manual stock change (+/-), logged with a reason. */
    fun adjustStock(materialId: Long, delta: Double, reason: MovementReason, note: String = "") =
        viewModelScope.launch { inventory.adjustStock(materialId, delta, reason, note) }
}
