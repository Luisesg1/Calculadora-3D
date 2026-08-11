package com.print3d.calculator.navigation

object Routes {
    const val HOME = "home"
    const val CALCULATOR = "calculator?quoteId={quoteId}"
    const val HISTORY = "history"
    const val MATERIALS = "materials"
    const val MACHINES = "machines"
    const val CLIENTS = "clients"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val AUTH = "auth"

    /** Top-level destinations that show the bottom navigation bar. */
    val TOP_LEVEL = setOf(HOME, HISTORY, MATERIALS, SETTINGS)

    fun calculator(quoteId: Long? = null) =
        "calculator?quoteId=${quoteId ?: -1L}"
}
