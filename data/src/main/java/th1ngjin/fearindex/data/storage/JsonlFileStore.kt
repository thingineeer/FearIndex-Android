package th1ngjin.fearindex.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * JSONL(줄 단위 JSON) 파일 저장소 — 알림 내역 등 소용량 append-only 데이터용 (iOS `JSONLFileStore` 대응).
 *
 * - 라인 인코딩/디코딩은 호출자 책임(예: [NotificationRecordCodec]) — 여기선 raw 문자열 라인만 다룬다.
 * - 모든 접근은 [Mutex] 로 직렬화, 파일 IO 는 [Dispatchers.IO].
 * - [rewrite] 는 임시 파일 → rename 원자적 교체.
 */
class JsonlFileStore(private val file: File) {

    private val mutex = Mutex()

    /** 한 줄 append (파일/디렉토리 없으면 생성). 개행은 여기서 붙인다. */
    suspend fun appendLine(line: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            ensureParentDirectory()
            file.appendText(line + "\n")
        }
    }

    /** 전체 라인 (빈 줄·공백 줄 제외). 파일 없으면 빈 리스트. */
    suspend fun readAllLines(): List<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!file.exists()) emptyList() else file.readLines().filter { it.isNotBlank() }
        }
    }

    /** 전체 교체 — 임시 파일에 쓴 뒤 rename 으로 원자적 교체 */
    suspend fun rewrite(lines: List<String>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            ensureParentDirectory()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(lines.joinToString(separator = "") { it + "\n" })
            moveAtomically(temp, file)
        }
    }

    private fun ensureParentDirectory() {
        file.parentFile?.mkdirs()
    }

    private fun moveAtomically(source: File, target: File) {
        try {
            Files.move(
                source.toPath(), target.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
