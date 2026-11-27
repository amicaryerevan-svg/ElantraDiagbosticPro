package com.elantradiagnostic.pro

/** Information reported by an ELM327-compatible adapter; it is not hardware authentication. */
internal data class Elm327AdapterDetails(
    val reportedIdentifier: String?,
    val identifierSource: String?,
    val protocolDescription: String?
)

internal object Elm327AdapterInfoParser {

    fun parse(
        resetBanner: String?,
        atiResponse: String?,
        protocolResponse: String?
    ): Elm327AdapterDetails {
        val atiIdentifier = firstUsableLine(atiResponse)
        val resetIdentifier = firstUsableLine(resetBanner)
        return Elm327AdapterDetails(
            reportedIdentifier = atiIdentifier ?: resetIdentifier,
            identifierSource = when {
                atiIdentifier != null -> "ATI"
                resetIdentifier != null -> "ATZ"
                else -> null
            },
            protocolDescription = firstUsableLine(protocolResponse)
        )
    }

    fun format(
        resetBanner: String?,
        atiResponse: String?,
        protocolResponse: String?
    ): String {
        val details = parse(resetBanner, atiResponse, protocolResponse)
        val identifier = details.reportedIdentifier ?: "не сообщён адаптером"
        val source = details.identifierSource ?: "источник не определён"
        val protocol = details.protocolDescription ?: "не сообщён адаптером"

        return "Идентификатор адаптера ($source): $identifier\n" +
            "Протокол адаптера (ATDP): $protocol\n" +
            "Важно: ATI/ATZ и ATDP возвращают строки, сообщаемые самим адаптером. " +
            "Они не подтверждают подлинность микросхемы ELM327 и не доказывают совместимость со всеми блоками автомобиля."
    }

    private fun firstUsableLine(response: String?): String? {
        val ignored = setOf("?", "ERROR", "NO DATA", "STOPPED", "ATI", "ATDP", "ATZ")
        return response
            ?.replace('>', ' ')
            ?.split(Regex("[\\r\\n]+"))
            ?.asSequence()
            ?.map { it.trim().replace(Regex("\\s+"), " ") }
            ?.firstOrNull { line ->
                line.isNotBlank() &&
                    line.uppercase(java.util.Locale.US) !in ignored &&
                    !line.contains("NO DATA", ignoreCase = true) &&
                    !line.contains("ERROR", ignoreCase = true)
            }
            ?.take(120)
    }
}
