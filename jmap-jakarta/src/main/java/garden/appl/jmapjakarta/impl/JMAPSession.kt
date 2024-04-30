package garden.appl.jmapjakarta.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class JMAPSession private constructor(
    private val sessionResourceUrl: String,
    val apiUrl: String,
    val downloadUrl: String,
    val uploadUrl: String,
    val authorization: String,
    val corePrimaryAccount: String,
    val mailPrimaryAccount: String,
    val submissionPrimaryAccount: String
) {
    companion object {
        val httpClient = OkHttpClient()
        val sessionCache = HashMap<Pair<String, String>, JMAPSession>(1)
        val format = Json { ignoreUnknownKeys = true }

        private suspend fun getSession(sessionResourceUrl: String, authorization: String): JMAPSession {
            val cachedSession = sessionCache[Pair(sessionResourceUrl, authorization)]
            if (cachedSession != null)
                return cachedSession

            val request = Request.Builder().run {
                url(sessionResourceUrl)
                addHeader("Authorization", authorization)
                get()
                build()
            }
            val jsonResponse = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        throw IOException("Unexpected response $response")

                    format.parseToJsonElement(response.body.string())
                }
            }

            return JMAPSession(
                sessionResourceUrl = sessionResourceUrl,
                apiUrl = jsonResponse.jsonObject["apiUrl"]!!.jsonPrimitive.content,
                downloadUrl = jsonResponse.jsonObject["downloadUrl"]!!.jsonPrimitive.content,
                uploadUrl = jsonResponse.jsonObject["uploadUrl"]!!.jsonPrimitive.content,
                authorization = authorization,
                corePrimaryAccount = jsonResponse.jsonObject["primaryAccounts"]!!
                    .jsonObject[JMAPConstants.CORE]!!.jsonPrimitive.content,
                mailPrimaryAccount = jsonResponse.jsonObject["primaryAccounts"]!!
                    .jsonObject[JMAPConstants.MAIL]!!.jsonPrimitive.content,
                submissionPrimaryAccount = jsonResponse.jsonObject["primaryAccounts"]!!
                    .jsonObject[JMAPConstants.SUBMISSION]!!.jsonPrimitive.content
            )
        }
    }
}