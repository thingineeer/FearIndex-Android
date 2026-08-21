package th1ngjin.fearindex.core.appcheck

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCheckFailureClassifierTest {

    @Test
    fun `403 App attestation failed → ATTESTATION_REJECTED`() {
        val kind = AppCheckFailureClassifier.classify(
            "Error returned from API. code: 403 body: App attestation failed.",
        )
        assertEquals(AppCheckFailureKind.ATTESTATION_REJECTED, kind)
    }

    @Test
    fun `Play Integrity 에러 코드(-4 PLAY_STORE_NOT_FOUND 등) → PLAY_INTEGRITY_UNAVAILABLE`() {
        listOf(
            "Integrity API error (-4): PLAY_STORE_NOT_FOUND",
            "Integrity API error (-3): API_NOT_AVAILABLE",
            "Integrity API error (-14): PLAY_SERVICES_VERSION_OUTDATED",
            "Integrity API error (-13): PLAY_STORE_VERSION_OUTDATED",
        ).forEach { message ->
            assertEquals(
                message,
                AppCheckFailureKind.PLAY_INTEGRITY_UNAVAILABLE,
                AppCheckFailureClassifier.classify(message),
            )
        }
    }

    @Test
    fun `네트워크 계열(-12, NETWORK_ERROR, timeout, UnknownHost) → NETWORK`() {
        listOf(
            "Integrity API error (-12): GOOGLE_SERVER_UNAVAILABLE",
            "Integrity API error (-5): NETWORK_ERROR",
            "Unable to resolve host firebaseappcheck.googleapis.com",
            "timeout",
        ).forEach { message ->
            assertEquals(message, AppCheckFailureKind.NETWORK, AppCheckFailureClassifier.classify(message))
        }
    }

    @Test
    fun `Play Integrity 호출 제한(-9 TOO_MANY_REQUESTS) → THROTTLED`() {
        assertEquals(
            AppCheckFailureKind.THROTTLED,
            AppCheckFailureClassifier.classify("Integrity API error (-9): TOO_MANY_REQUESTS"),
        )
    }

    @Test
    fun `알 수 없는 메시지와 null → UNKNOWN`() {
        assertEquals(AppCheckFailureKind.UNKNOWN, AppCheckFailureClassifier.classify("something else"))
        assertEquals(AppCheckFailureKind.UNKNOWN, AppCheckFailureClassifier.classify(null))
    }
}
