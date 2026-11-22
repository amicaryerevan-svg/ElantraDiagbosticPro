package com.elantradiagnostic.pro

/**
 * Small, explicitly generic DTC description set.
 *
 * Only selected standardized P0xxx powertrain codes are described. Manufacturer-
 * specific codes and codes outside this list deliberately remain unresolved rather
 * than being guessed from a superficially similar code.
 */
internal object Obd2DtcDescriptions {

    private val descriptions = mapOf(
        "P0101" to "Диапазон/характеристика сигнала датчика массового или объёмного расхода воздуха (MAF).",
        "P0113" to "Высокий уровень сигнала датчика температуры впускного воздуха (IAT).",
        "P0128" to "Температура охлаждающей жидкости ниже ожидаемой температуры регулирования термостата.",
        "P0133" to "Медленный отклик кислородного датчика Bank 1 Sensor 1.",
        "P0171" to "Система слишком бедная, Bank 1.",
        "P0172" to "Система слишком богатая, Bank 1.",
        "P0300" to "Обнаружены случайные или множественные пропуски воспламенения.",
        "P0301" to "Обнаружены пропуски воспламенения в цилиндре 1.",
        "P0302" to "Обнаружены пропуски воспламенения в цилиндре 2.",
        "P0303" to "Обнаружены пропуски воспламенения в цилиндре 3.",
        "P0304" to "Обнаружены пропуски воспламенения в цилиндре 4.",
        "P0420" to "Эффективность каталитической системы ниже порогового значения, Bank 1.",
        "P0442" to "Обнаружена небольшая утечка системы улавливания паров топлива (EVAP).",
        "P0455" to "Обнаружена крупная утечка системы улавливания паров топлива (EVAP).",
        "P0456" to "Обнаружена очень небольшая утечка системы улавливания паров топлива (EVAP).",
        "P0500" to "Неисправность сигнала датчика скорости автомобиля.",
        "P0562" to "Низкое напряжение бортовой системы питания."
    )

    /**
     * Returns a generic standardized description only for known P0xxx codes.
     * Unknown and manufacturer-specific codes intentionally return null.
     */
    fun descriptionFor(code: String): String? {
        val normalized = code.trim().uppercase()
        if (normalized.length != 5 ||
            !normalized.startsWith("P0") ||
            !normalized.drop(2).all { it in '0'..'9' }
        ) {
            return null
        }
        return descriptions[normalized]
    }
}
