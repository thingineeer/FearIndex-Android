package th1ngjin.fearindex.data.storage

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** iOS `JSONLFileStoreTests` 포팅 (raw line 계층만 — Codable 편의는 NotificationRecordCodec 담당). */
class JsonlFileStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun makeStore(): Pair<JsonlFileStore, File> {
        val file = File(temp.root, "nested/dir/store.jsonl")
        return JsonlFileStore(file) to file
    }

    @Test
    fun `appendLine 후 readAllLines 로 라인 왕복 (파일과 상위 디렉토리 자동 생성)`() = runTest {
        val (store, file) = makeStore()
        store.appendLine("""{"a":1}""")
        store.appendLine("""{"b":2}""")
        assertEquals(listOf("""{"a":1}""", """{"b":2}"""), store.readAllLines())
        assertTrue(file.exists())
    }

    @Test
    fun `파일 없으면 readAllLines 는 빈 리스트 (예외 없음)`() = runTest {
        val (store, file) = makeStore()
        assertTrue(store.readAllLines().isEmpty())
        assertFalse(file.exists())
    }

    @Test
    fun `빈 줄과 공백 줄은 건너뛰고 나머지만 돌려준다`() = runTest {
        val (store, file) = makeStore()
        store.appendLine("""{"id":"ok"}""")
        file.appendText("\n   \nnot-json\n\n")
        store.appendLine("""{"id":"ok2"}""")
        assertEquals(listOf("""{"id":"ok"}""", "not-json", """{"id":"ok2"}"""), store.readAllLines())
    }

    @Test
    fun `rewrite 는 전체 원자적 교체 - 빈 리스트면 빈 파일, 임시 파일은 남지 않는다`() = runTest {
        val (store, file) = makeStore()
        store.appendLine("old")
        store.rewrite(listOf("new1", "new2"))
        assertEquals(listOf("new1", "new2"), store.readAllLines())
        assertEquals("new1\nnew2\n", file.readText())
        store.rewrite(emptyList())
        assertTrue(store.readAllLines().isEmpty())
        assertTrue(file.exists())
        assertEquals(listOf("store.jsonl"), file.parentFile!!.list()!!.toList())
    }

    @Test
    fun `rewrite 는 파일이 없어도 디렉토리를 만들고 쓴다`() = runTest {
        val (store, file) = makeStore()
        store.rewrite(listOf("x"))
        assertEquals(listOf("x"), store.readAllLines())
        assertTrue(file.exists())
    }
}
