package com.print3d.calculator.domain.catalog

/**
 * Static suggestion catalogs for the material and machine editors. Purely advisory —
 * every field stays free-text, so users can add anything not listed here.
 */
object Catalog {

    /** Common FDM filament types. */
    val filamentTypes = listOf(
        "PLA", "PLA+", "PLA Silk", "PLA Matte", "PETG", "ABS", "ASA", "TPU",
        "Nylon (PA)", "PC (Policarbonato)", "PVA", "HIPS", "PET", "PP",
        "Wood / Madera", "Carbon Fiber", "Glow / Fosforescente", "Metal Fill",
        "Resina Standard", "Resina Tough", "Resina Water Washable"
    )

    /** Common filament brands. */
    val filamentBrands = listOf(
        "Bambu Lab", "eSun", "Polymaker", "Prusament", "Overture", "Sunlu",
        "Hatchbox", "Creality", "Elegoo", "Anycubic", "Fillamentum", "ColorFabb",
        "Amolen", "Geeetech", "3DFils", "Grilon3", "Smartfil", "FiberForce"
    )

    /** Printer brand → known models. Selecting a brand filters the model suggestions. */
    val printerBrands: Map<String, List<String>> = linkedMapOf(
        "Bambu Lab" to listOf("A1 mini", "A1", "P1P", "P1S", "X1", "X1 Carbon", "X1E", "H2D"),
        "Creality" to listOf(
            "Ender 3", "Ender 3 V2", "Ender 3 S1", "Ender 3 S1 Pro", "Ender 3 V3 SE",
            "Ender 3 V3", "Ender 5", "CR-10", "CR-10 Smart", "K1", "K1 Max", "K2 Plus"
        ),
        "Prusa" to listOf("MINI+", "MK3S+", "MK4", "MK4S", "XL", "CORE One"),
        "Anycubic" to listOf(
            "Kobra", "Kobra 2", "Kobra 2 Pro", "Kobra 2 Max", "Kobra 3", "Vyper",
            "Mega S", "Photon Mono", "Photon Mono X"
        ),
        "Elegoo" to listOf("Neptune 3", "Neptune 3 Pro", "Neptune 4", "Neptune 4 Pro", "Neptune 4 Max", "Mars 4", "Saturn 4"),
        "Sovol" to listOf("SV06", "SV06 Plus", "SV07", "SV08"),
        "Artillery" to listOf("Sidewinder X2", "Sidewinder X4", "Genius Pro"),
        "Voron" to listOf("V0.2", "Trident", "V2.4"),
        "Qidi" to listOf("X-Plus 3", "X-Max 3", "Q1 Pro"),
        "FlashForge" to listOf("Adventurer 5M", "Adventurer 5M Pro", "Creator Pro 2"),
        "Otra / Other" to emptyList()
    )

    val printerBrandNames: List<String> = printerBrands.keys.toList()

    fun modelsForBrand(brand: String): List<String> =
        printerBrands[brand] ?: printerBrands.values.flatten()

    val allPrinterModels: List<String> = printerBrands.values.flatten()
}
