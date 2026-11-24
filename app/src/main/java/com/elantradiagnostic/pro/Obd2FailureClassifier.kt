package com.elantradiagnostic.pro

import java.io.IOException

internal enum class Obd2OperationStage {
    CONNECTION,
    ADAPTER_INITIALIZATION,
    ECU_REQUEST
}

internal class Elm327NoResponseException(command: String) :
    IOException("Нет ответа от адаптера на команду $command.")

internal class Elm327IncompleteResponseException(command: String, partial: String) :
    IOException(
        "Ответ на команду $command не завершён: адаптер не прислал приглашение '>'. " +
            "Получено: ${partial.take(120)}"
    )

/** Maps transport failures to cautious, user-facing history categories. */
internal object Obd2FailureClassifier {
    fun classify(stage: Obd2OperationStage, error: Exception): Obd2DiagnosticStatus =
        when {
            error is SecurityException -> Obd2DiagnosticStatus.PERMISSION_DENIED
            error is Elm327IncompleteResponseException -> Obd2DiagnosticStatus.INCOMPLETE_RESPONSE
            error is Elm327NoResponseException && stage == Obd2OperationStage.ADAPTER_INITIALIZATION ->
                Obd2DiagnosticStatus.ADAPTER_NO_RESPONSE
            error is Elm327NoResponseException ->
                Obd2DiagnosticStatus.ECU_NO_RESPONSE
            stage == Obd2OperationStage.CONNECTION ->
                Obd2DiagnosticStatus.CONNECTION_FAILED
            stage == Obd2OperationStage.ADAPTER_INITIALIZATION ->
                Obd2DiagnosticStatus.ADAPTER_ERROR
            else -> Obd2DiagnosticStatus.ECU_ERROR
        }
}
