package com.print3d.calculator.core.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import com.print3d.calculator.domain.model.AppLanguage
import java.util.Locale

/**
 * Wraps a base context with an overridden locale for in-app language switching.
 *
 * Must return a [ContextWrapper] around [base] (an Activity) rather than a raw
 * `createConfigurationContext` result — otherwise `hiltViewModel()` cannot unwrap to
 * the Activity and throws "Expected an activity context for creating a HiltViewModelFactory".
 */
object LocaleHelper {
    fun wrap(base: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return base
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        val localized = base.createConfigurationContext(config)
        return object : ContextWrapper(base) {
            override fun getResources(): Resources = localized.resources
            override fun getAssets(): AssetManager = localized.assets
        }
    }
}
