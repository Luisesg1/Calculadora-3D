package com.print3d.calculator.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.print3d.calculator.domain.model.AppCurrency
import com.print3d.calculator.domain.model.AppLanguage
import com.print3d.calculator.domain.model.AppThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

data class AppSettings(
    val theme: AppThemeMode = AppThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val currency: AppCurrency = AppCurrency.CLP,
    val electricityRate: Double = 150.0,
    val defaultMargin: Double = 40.0,
    val defaultTax: Double = 0.0,
    val dateFormat: String = "dd/MM/yyyy",
    val businessName: String = "",
    val businessPhone: String = "",
    val businessEmail: String = "",
    val businessInstagram: String = "",
    val businessFacebook: String = "",
    val businessWeb: String = "",
    val businessAddress: String = "",
    val logoUri: String = "",
    val onboardingDone: Boolean = false,
    val accountName: String = "",
    val accountEmail: String = ""
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENCY = stringPreferencesKey("currency")
        val ELECTRICITY = doublePreferencesKey("electricity_rate")
        val MARGIN = doublePreferencesKey("default_margin")
        val TAX = doublePreferencesKey("default_tax")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val NAME = stringPreferencesKey("biz_name")
        val PHONE = stringPreferencesKey("biz_phone")
        val EMAIL = stringPreferencesKey("biz_email")
        val INSTAGRAM = stringPreferencesKey("biz_ig")
        val FACEBOOK = stringPreferencesKey("biz_fb")
        val WEB = stringPreferencesKey("biz_web")
        val ADDRESS = stringPreferencesKey("biz_address")
        val LOGO = stringPreferencesKey("biz_logo")
        val ONBOARDING = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_done")
        val ACCOUNT_NAME = stringPreferencesKey("account_name")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = runCatching { AppThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }
                .getOrDefault(AppThemeMode.SYSTEM),
            language = AppLanguage.fromTag(p[Keys.LANGUAGE]),
            currency = AppCurrency.fromCode(p[Keys.CURRENCY]),
            electricityRate = p[Keys.ELECTRICITY] ?: 150.0,
            defaultMargin = p[Keys.MARGIN] ?: 40.0,
            defaultTax = p[Keys.TAX] ?: 0.0,
            dateFormat = p[Keys.DATE_FORMAT] ?: "dd/MM/yyyy",
            businessName = p[Keys.NAME] ?: "",
            businessPhone = p[Keys.PHONE] ?: "",
            businessEmail = p[Keys.EMAIL] ?: "",
            businessInstagram = p[Keys.INSTAGRAM] ?: "",
            businessFacebook = p[Keys.FACEBOOK] ?: "",
            businessWeb = p[Keys.WEB] ?: "",
            businessAddress = p[Keys.ADDRESS] ?: "",
            logoUri = p[Keys.LOGO] ?: "",
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            accountName = p[Keys.ACCOUNT_NAME] ?: "",
            accountEmail = p[Keys.ACCOUNT_EMAIL] ?: ""
        )
    }

    suspend fun setTheme(v: AppThemeMode) = edit { it[Keys.THEME] = v.name }
    suspend fun setLanguage(v: AppLanguage) = edit { it[Keys.LANGUAGE] = v.tag }
    suspend fun setCurrency(v: AppCurrency) = edit { it[Keys.CURRENCY] = v.code }
    suspend fun setElectricityRate(v: Double) = edit { it[Keys.ELECTRICITY] = v }
    suspend fun setDefaultMargin(v: Double) = edit { it[Keys.MARGIN] = v }
    suspend fun setDefaultTax(v: Double) = edit { it[Keys.TAX] = v }
    suspend fun setDateFormat(v: String) = edit { it[Keys.DATE_FORMAT] = v }
    suspend fun setLogoUri(v: String) = edit { it[Keys.LOGO] = v }
    suspend fun setOnboardingDone(v: Boolean) = edit { it[Keys.ONBOARDING] = v }
    suspend fun setAccount(name: String, email: String) = edit {
        it[Keys.ACCOUNT_NAME] = name
        it[Keys.ACCOUNT_EMAIL] = email
    }

    suspend fun setBusiness(
        name: String, phone: String, email: String, instagram: String,
        facebook: String, web: String, address: String
    ) = edit {
        it[Keys.NAME] = name
        it[Keys.PHONE] = phone
        it[Keys.EMAIL] = email
        it[Keys.INSTAGRAM] = instagram
        it[Keys.FACEBOOK] = facebook
        it[Keys.WEB] = web
        it[Keys.ADDRESS] = address
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
