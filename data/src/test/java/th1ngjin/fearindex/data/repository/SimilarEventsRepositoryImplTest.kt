package th1ngjin.fearindex.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import th1ngjin.fearindex.core.debug.ScreenshotMode
import th1ngjin.fearindex.domain.entity.FearIndexType
import javax.inject.Provider

class SimilarEventsRepositoryImplTest {

    private val firestore = mockk<FirebaseFirestore>(relaxed = true)
    private val functions = mockk<FirebaseFunctions>(relaxed = true)
    private val repository = SimilarEventsRepositoryImpl(
        Provider { firestore },
        Provider { functions },
    )

    @Test
    fun `screenshot mode - observe는 Firestore 없이 fixture를 반환한다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)

        try {
            val result = repository.observe(FearIndexType.KOSPI).first()

            assertEquals(FearIndexType.KOSPI, result.indexType)
            assertTrue(result.matches.isNotEmpty())
            verify(exactly = 0) { firestore.collection(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }

    @Test
    fun `screenshot mode - triggerCallable은 Functions를 호출하지 않는다`() = runTest {
        ScreenshotMode.setOverrideForTesting(true)

        try {
            repository.triggerCallable(FearIndexType.MARKET, 27)

            coVerify(exactly = 0) { functions.getHttpsCallable(any()) }
        } finally {
            ScreenshotMode.setOverrideForTesting(null)
        }
    }
}
