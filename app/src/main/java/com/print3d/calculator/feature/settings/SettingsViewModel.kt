package com.print3d.calculator.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.print3d.calculator.data.settings.AppSettings
import com.print3d.calculator.data.settings.SettingsRepository
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.AppLanguage
import com.print3d.calculator.domain.model.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    /** null until DataStore delivers the first real value; prevents onboarding flashing on cold start. */
    val settings: StateFlow<AppSettings?> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTheme(v: AppThemeMode) = viewModelScope.launch { repo.setTheme(v) }
    fun setLanguage(v: AppLanguage) = viewModelScope.launch { repo.setLanguage(v) }
    fun setCurrency(v: AppCurrency) = viewModelScope.launch { repo.setCurrency(v) }
    fun setElectricity(v: Double) = viewModelScope.launch { repo.setElectricityRate(v) }
    fun setMargin(v: Double) = viewModelScope.launch { repo.setDefaultMargin(v) }
    fun setTax(v: Double) = viewModelScope.launch { repo.setDefaultTax(v) }
    fun setLogo(uri: String) = viewModelScope.launch { repo.setLogoUri(uri) }
    /** Clears the account and returns to the initial login/onboarding flow. Local data is kept. */
    fun signOut() = viewModelScope.launch {
        repo.setAccount("", "")
        repo.setOnboardingDone(false)
    }
    fun setAccount(name: String, email: String) = viewModelScope.launch { repo.setAccount(name.trim(), email.trim()) }
    fun setBusiness(
        name: String, phone: String, email: String, instagram: String,
        facebook: String, web: String, address: String
    ) = viewModelScope.launch {
        repo.setBusiness(name, phone, email, instagram, facebook, web, address)
    }
}
