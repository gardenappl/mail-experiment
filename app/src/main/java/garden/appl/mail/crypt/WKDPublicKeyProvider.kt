package garden.appl.mail.crypt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.jcajce.provider.digest.SHA1
import org.bouncycastle.jcajce.util.MessageDigestUtils
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.pgpainless.PGPainless
import se.welcomweb.utils.zbase32j.ZBase32j
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

class WKDPublicKeyProvider : PublicKeyProvider() {
    companion object {
        private val zBase32 = ZBase32j()
        private const val LOGGING_TAG = "WKDPublicKeyProvider"
    }

    override suspend fun getKeyRing(address: String, context: Context): PGPPublicKeyRing? {
        val parts = address.split('@', limit = 2)
        if (parts.size != 2)
            return null
        val (local, domain) = parts
        if (!domain.contains('.'))
            return null

        val sha1local = MessageDigest.getInstance("SHA-1").let { crypt ->
            crypt.reset()
            crypt.update(local.toByteArray())

            return@let crypt.digest()
        }
        val localPathSegment = zBase32.encode(sha1local)

        tryGetKeyFrom(URL("https://$domain/.well-known/openpgpkey/hu/$localPathSegment"))
            ?.let { key -> return key }
        return tryGetKeyFrom(URL("https://openpgpkey.$domain/.well-known/openpgpkey/hu/$domain/$localPathSegment"))
    }

    private suspend fun tryGetKeyFrom(url: URL): PGPPublicKeyRing? {
        Log.d(LOGGING_TAG, "Accessing URL $url")

        return withContext(Dispatchers.IO) {
            try {
                with(url.openConnection() as HttpsURLConnection) {
                    requestMethod = "GET"
                    connect()

                    if (this.responseCode != 200)
                        return@withContext null

                    return@withContext PGPainless.readKeyRing().publicKeyRing(this.inputStream)
                }
            } catch (e: IOException) {
                Log.w(LOGGING_TAG, "Failed to connect to: $e")
                return@withContext null
            }
        }
    }
}