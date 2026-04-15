package th1ngjin.fearindex.data.storage

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import th1ngjin.fearindex.data.datasource.VoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Buy/Hold/Sell 투표 관련 영속 저장소.
 *
 * - 오늘 내가 투표했는지 여부 (indexType별)
 * - 내 투표 선택지 캐시
 */
@Singleton
class VoteStorage @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 오늘 이미 투표했는지 확인.
     * date가 다르면 이전 날짜이므로 false 반환.
     */
    fun hasVotedToday(indexType: String): Boolean {
        val savedDate = prefs.getString(voteDateKey(indexType), null)
        val today = VoteDataSource.todayUTC()
        return savedDate == today
    }

    /** 오늘 내 투표 선택지 ("buy" / "hold" / "sell") */
    fun loadMyVote(indexType: String): String? {
        if (!hasVotedToday(indexType)) return null
        return prefs.getString(voteChoiceKey(indexType), null)
    }

    fun saveMyVote(indexType: String, choice: String) {
        val today = VoteDataSource.todayUTC()
        prefs.edit()
            .putString(voteDateKey(indexType), today)
            .putString(voteChoiceKey(indexType), choice)
            .apply()
    }

    private fun voteDateKey(indexType: String) = "vote_date_$indexType"
    private fun voteChoiceKey(indexType: String) = "vote_choice_$indexType"

    companion object {
        private const val PREFS_NAME = "vote_prefs"
    }
}
