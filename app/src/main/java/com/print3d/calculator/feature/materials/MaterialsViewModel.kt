package com.print3d.calculator.feature.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.MaterialRepository
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.Material
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialsViewModel @Inject constructor(
    private val repo: MaterialRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    val materials: StateFlow<List<Material>> = repo.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currency: StateFlow<AppCurrency> = settingsRepo.settings
        .map { it.currency }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppCurrency.CLP)

    fun save(material: Material) = viewModelScope.launch { repo.save(material) }
    fun delete(material: Material) = viewModelScope.launch { repo.delete(material) }
    fun duplicate(material: Material) = viewModelScope.launch {
        repo.save(material.copy(id = 0, name = material.name + " (copia)"))
    }
}
