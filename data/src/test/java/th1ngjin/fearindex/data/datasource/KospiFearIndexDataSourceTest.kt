package th1ngjin.fearindex.data.datasource

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import th1ngjin.fearindex.data.dto.KospiPublicSnapshotResponse

class KospiFearIndexDataSourceTest {

    private val api = mockk<KospiFearIndexApi>()
    private val dataSource = KospiFearIndexDataSource(api)

    @Test
    fun `fetchSnapshot - current 요청은 version만 전달한다`() = runTest {
        val expected = createResponse()
        coEvery { api.getKospiFearIndex(version = "20260610", history = null) } returns expected

        val result = dataSource.fetchSnapshot(includeHistory = false)

        assertSame(expected, result)
        coVerify(exactly = 1) { api.getKospiFearIndex(version = "20260610", history = null) }
    }

    @Test
    fun `fetchSnapshot - history 요청은 history=1을 전달한다`() = runTest {
        val expected = createResponse()
        coEvery { api.getKospiFearIndex(version = "20260610", history = 1) } returns expected

        dataSource.fetchSnapshot(includeHistory = true)

        coVerify(exactly = 1) { api.getKospiFearIndex(version = "20260610", history = 1) }
    }

    @Test
    fun `fetchSnapshot - current와 history cache는 분리된다`() = runTest {
        val current = createResponse()
        val history = createResponse()
        coEvery { api.getKospiFearIndex(version = "20260610", history = null) } returns current
        coEvery { api.getKospiFearIndex(version = "20260610", history = 1) } returns history

        dataSource.fetchSnapshot(includeHistory = false)
        dataSource.fetchSnapshot(includeHistory = false)
        dataSource.fetchSnapshot(includeHistory = true)
        dataSource.fetchSnapshot(includeHistory = true)

        coVerify(exactly = 1) { api.getKospiFearIndex(version = "20260610", history = null) }
        coVerify(exactly = 1) { api.getKospiFearIndex(version = "20260610", history = 1) }
    }

    @Test
    fun `fetchSnapshot - 동시에 들어온 같은 history 요청은 네트워크 1회만 호출한다`() = runTest {
        val expected = createResponse()
        val gate = CompletableDeferred<Unit>()
        coEvery { api.getKospiFearIndex(version = "20260610", history = 1) } coAnswers {
            gate.await()
            expected
        }

        val first = async { dataSource.fetchSnapshot(includeHistory = true) }
        val second = async { dataSource.fetchSnapshot(includeHistory = true) }
        gate.complete(Unit)

        assertSame(expected, first.await())
        assertSame(expected, second.await())
        coVerify(exactly = 1) { api.getKospiFearIndex(version = "20260610", history = 1) }
    }

    @Test
    fun `fetchSnapshot - forceRefresh면 cache를 우회한다`() = runTest {
        val response = createResponse()
        coEvery { api.getKospiFearIndex(version = "20260610", history = null) } returns response

        dataSource.fetchSnapshot(includeHistory = false)
        dataSource.fetchSnapshot(includeHistory = false, forceRefresh = true)

        coVerify(exactly = 2) { api.getKospiFearIndex(version = "20260610", history = null) }
    }

    private fun createResponse() = KospiPublicSnapshotResponse(
        version = 2,
        generatedAt = "2026-06-11T01:00:06.211Z",
        latest = null,
        history = emptyList(),
        chartHistory = null,
        historyCount = null,
    )
}
