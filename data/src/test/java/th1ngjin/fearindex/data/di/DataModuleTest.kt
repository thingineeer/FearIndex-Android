package th1ngjin.fearindex.data.di

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModuleTest {

    @Test
    fun `provideCNNOkHttpClient - iOS와 같은 CNN browser headers를 추가한다`() {
        var capturedRequest: Request? = null
        val baseClient = OkHttpClient()
        val client = DataModule.provideCNNOkHttpClient(baseClient)
            .newBuilder()
            .addInterceptor(captureInterceptor { capturedRequest = it })
            .build()

        client.newCall(
            Request.Builder()
                .url("https://production.dataviz.cnn.io/index/fearandgreed/graphdata/2026-03-14")
                .build(),
        ).execute().close()

        val request = checkNotNull(capturedRequest)
        assertTrue(request.header("User-Agent").orEmpty().contains("Mozilla/5.0"))
        assertTrue(request.header("User-Agent").orEmpty().contains("Safari"))
        assertEquals("https://www.cnn.com/markets/fear-and-greed", request.header("Referer"))
        assertEquals("https://www.cnn.com", request.header("Origin"))
        assertEquals("*/*", request.header("Accept"))
        assertEquals("en-US,en;q=0.9", request.header("Accept-Language"))
    }

    private fun captureInterceptor(capture: (Request) -> Unit) = Interceptor { chain ->
        capture(chain.request())
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody())
            .build()
    }
}
