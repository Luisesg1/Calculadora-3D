package com.print3d.calculator.domain.model

/** Supported display currencies. Add new entries here — everything downstream is data-driven. */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val decimals: Int,
    val localeTag: String,
    val displayName: String,
    /**
     * Display-only monthly Pro price shown on the upsell for this currency. These are what
     * the card shows; the amount Google actually charges is set per-market in Play Console —
     * keep these roughly aligned with those, they are not authoritative.
     */
    val proMonthly: Double
) {
    CLP("CLP", "$", 0, "es-CL", "Peso chileno", 1900.0),
    USD("USD", "$", 2, "en-US", "US Dollar", 1.99),
    EUR("EUR", "€", 2, "es-ES", "Euro", 1.99),
    MXN("MXN", "$", 2, "es-MX", "Peso mexicano", 39.0),
    ARS("ARS", "$", 2, "es-AR", "Peso argentino", 2500.0),
    PEN("PEN", "S/", 2, "es-PE", "Sol peruano", 7.90),
    COP("COP", "$", 0, "es-CO", "Peso colombiano", 8900.0),
    BRL("BRL", "R$", 2, "pt-BR", "Real brasileño", 9.90);

    companion object {
        fun fromCode(code: String?): AppCurrency =
            entries.firstOrNull { it.code == code } ?: CLP

        private val EURO_COUNTRIES = setOf(
            "ES", "PT", "DE", "FR", "IT", "AT", "BE", "NL", "IE", "FI",
            "GR", "SK", "SI", "EE", "LV", "LT", "LU", "MT", "CY"
        )

        /** Best-effort currency by device country. Never converts money — only picks the format. */
        fun fromCountry(country: String?): AppCurrency = when (country?.uppercase()) {
            "CL" -> CLP
            "US" -> USD
            "MX" -> MXN
            "AR" -> ARS
            "PE" -> PEN
            "CO" -> COP
            "BR" -> BRL
            in EURO_COUNTRIES -> EUR
            else -> CLP
        }

        fun detectDefault(): AppCurrency = fromCountry(java.util.Locale.getDefault().country)
    }
}

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    SPANISH("es"),
    ENGLISH("en"),
    PORTUGUESE("pt");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

/** Lifecycle of a quote, so the app works as a light CRM (draft → sent → accepted/rejected). */
enum class QuoteStatus {
    DRAFT, SENT, ACCEPTED, REJECTED;

    companion object {
        fun fromName(name: String?): QuoteStatus =
            entries.firstOrNull { it.name == name } ?: DRAFT
    }
}
