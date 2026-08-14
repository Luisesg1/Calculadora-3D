package com.print3d.calculator.core.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import com.print3d.calculator.domain.model.AppLanguage
import java.util.Locale

/**
 * Applies an in-app language override.
 *
 * Two layers are used together because a Compose-only `LocalContext` override is silently
 * ignored by some OEM skins (notably MIUI/HyperOS): the chosen locale is applied to the
 * Activity's *base context* in `attachBaseContext` (see MainActivity) and re-provided through
 * Compose. The base-context override is authoritative; the Compose one keeps popups/sheets
 * (which start their own subcomposition) in the same language.
 *
 * The tag is mirrored into a plain [android.content.SharedPreferences] so `attachBaseContext`
 * can read it synchronously — DataStore is async and not available that early in the lifecycle.
 *
 * Wrapping must yield a [ContextWrapper] around [base] (an Activity) rather than a raw
 * `createConfigurationContext` result — otherwise `hiltViewModel()` cannot unwrap to the
 * Activity and throws "Expected an activity context for creating a HiltViewModelFactory".
 */
object LocaleHelper {
    private const val PREFS = "locale_prefs"
    private const val KEY = "language_tag"

    /** Mirror the chosen language tag for synchronous reads in [persistedTag]. */
    fun persist(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
    }

    /** Language tag last chosen, or "" for SYSTEM. Safe to call before Compose is up. */
    fun persistedTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    fun wrap(base: Context, language: AppLanguage): Context = wrap(base, language.tag)

    fun wrap(base: Context, tag: String): Context {
        if (tag.isBlank()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        val localized = base.createConfigurationContext(config)
        return object : ContextWrapper(base) {
            override fun getResources(): Resources = localized.resources
            override fun getAssets(): AssetManager = localized.assets
        }
    }
}
