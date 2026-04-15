package th1ngjin.fearindex.core.util

import java.net.URLEncoder

object ShareUrlBuilder {

    private const val BASE_URL = "https://fear-index-a4f4b.web.app"

    fun build(score: Int, type: String, rating: String): String {
        val encodedRating = URLEncoder.encode(rating, "UTF-8")
        return "$BASE_URL/?score=$score&type=$type&rating=$encodedRating"
    }
}
