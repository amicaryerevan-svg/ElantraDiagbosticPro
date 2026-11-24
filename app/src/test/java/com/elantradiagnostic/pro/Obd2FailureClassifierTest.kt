package com.elantradiagnostic.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class Obd2FailureClassifierTest {

    @Test
    fun distinguishesConnectionAndAdapterNoResponse() {
        assertEquals(
            Obd2DiagnosticStatus.CONNECTION_FAILED,
            Obd2FailureClassifier.classify(
                Obd2OperationStage.CONNECTION,
                java.io.IOException("connect failed")
            )
        )
        assertEquals(
            Obd2DiagnosticStatus.ADAPTER_NO_RESPONSE,
            Obd2FailureClassifier.classify(
                Obd2OperationStage.ADAPTER_INITIALIZATION,
                Elm327NoResponseException("ATZ")
            )
        )
    }

    @Test
    fun distinguishesEcuNoResponseAndIncompleteReply() {
        assertEquals(
            Obd2DiagnosticStatus.ECU_NO_RESPONSE,
            Obd2FailureClassifier.classify(
                Obd2OperationStage.ECU_REQUEST,
                Elm327NoResponseException("0100")
            )
        )
        assertEquals(
            Obd2DiagnosticStatus.INCOMPLETE_RESPONSE,
            Obd2FailureClassifier.classify(
                Obd2OperationStage.ECU_REQUEST,
                Elm327IncompleteResponseException("0100", "41 00")
            )
        )
    }

    @Test
    fun permissionFailureHasItsOwnStatus() {
        assertEquals(
            Obd2DiagnosticStatus.PERMISSION_DENIED,
            Obd2FailureClassifier.classify(
                Obd2OperationStage.CONNECTION,
                SecurityException("permission denied")
            )
        )
    }
}
