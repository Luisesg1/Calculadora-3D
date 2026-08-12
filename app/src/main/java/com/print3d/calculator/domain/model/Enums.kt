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

/**
 * Lifecycle of a quote, so the app works as a light CRM.
 * Happy path: draft → sent → viewed → accepted → in production → delivered.
 * Off-ramps: rejected, cancelled.
 */
enum class QuoteStatus {
    DRAFT, SENT, VIEWED, ACCEPTED, IN_PRODUCTION, DELIVERED, REJECTED, CANCELLED;

    /** Terminal states — no further automatic progression. */
    val isTerminal: Boolean get() = this == DELIVERED || this == REJECTED || this == CANCELLED

    /** Next stage in the happy-path flow, or null if there is none. */
    fun next(): QuoteStatus? {
        val i = KANBAN_FLOW.indexOf(this)
        return if (i >= 0 && i < KANBAN_FLOW.lastIndex) KANBAN_FLOW[i + 1] else null
    }

    companion object {
        /** The forward pipeline used by the Kanban board (excludes the off-ramp states). */
        val KANBAN_FLOW = listOf(DRAFT, SENT, VIEWED, ACCEPTED, IN_PRODUCTION, DELIVERED)

        fun fromName(name: String?): QuoteStatus =
            entries.firstOrNull { it.name == name } ?: DRAFT
    }
}

/** How close a quote is to (or past) its due date. Derived from dueDate — never persisted. */
enum class DueState { NONE, VALID, DUE_SOON, OVERDUE }

/** Reason a material's stock changed. Drives the movement log. */
enum class MovementReason {
    PURCHASE, CONSUMPTION, CORRECTION, WASTE, RETURN, MANUAL;

    companion object {
        fun fromName(name: String?): MovementReason =
            entries.firstOrNull { it.name == name } ?: MANUAL
    }
}

/** Stock health of a material relative to its minimum. */
enum class StockStatus { OK, LOW, OUT }
