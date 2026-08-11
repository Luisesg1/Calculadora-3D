package com.print3d.calculator.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.repo.MachineRepository
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.AppLanguage
import com.print3d.calculator.domain.model.Machine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val machineRepo: MachineRepository
) : ViewModel() {

    /** Applied immediately so the following steps render in the chosen language. */
    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settingsRepo.setLanguage(language)
    }

    fun setCurrency(currency: AppCurrency) = viewModelScope.launch {
        settingsRepo.setCurrency(currency)
    }

    /** Persists every chosen printer and marks onboarding complete. */
    fun finish(printers: List<Pair<String, String>>) = viewModelScope.launch {
        printers.forEach { (rawBrand, rawModel) ->
            val brand = rawBrand.trim()
            val model = rawModel.trim()
            if (brand.isNotBlank() || model.isNotBlank()) {
                val name = listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ")
                machineRepo.save(Machine(name = name.ifBlank { "Impresora" }, brand = brand, model = model))
            }
        }
        settingsRepo.setOnboardingDone(true)
    }
}
