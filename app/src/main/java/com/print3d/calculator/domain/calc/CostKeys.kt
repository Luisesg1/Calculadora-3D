package com.print3d.calculator.domain.calc

/** Canonical keys for the built-in extra cost lines. `labor` is broken out in the result. */
object CostKeys {
    const val LABOR = "labor"

    /** Ordered built-in cost lines shown by default in the calculator. */
    val BUILT_IN = listOf(
        LABOR to "field_labor",
        "packaging" to "field_packaging",
        "shipping" to "field_shipping",
        "supplies" to "field_supplies",
        "paint" to "field_paint",
        "sanding" to "field_sanding",
        "glue" to "field_glue",
        "magnets" to "field_magnets",
        "screws" to "field_screws",
        "bearings" to "field_bearings",
        "electronics" to "field_electronics"
    )
}
