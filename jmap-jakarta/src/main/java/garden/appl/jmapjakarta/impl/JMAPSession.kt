package garden.appl.jmapjakarta.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.http2.Header
import java.io.IOException

internal class JMAPSession private constructor(
    private val url: String,
    private val authorization: String
) {
    companion object {
        val httpClient = OkHttpClient()
        val sessionCache = HashMap<Pair<String, String>, JMAPSession>(1)

        private suspend fun getSession(url: String, authorization: String): JMAPSession {
            val cachedSession = sessionCache[Pair(url, authorization)]
            if (cachedSession != null)
                return cachedSession

            val request = Request.Builder().run {
                url(url)
                addHeader("Authorization", authorization)
                get()
                build()
            }
            val jsonResponse = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        throw IOException("Unexpected response $response")

                    Json.parseToJsonElement(response.body.string())
                }
            }
            jsonResponse.jsonObject
        }
    }
}