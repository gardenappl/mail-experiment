package garden.appl.mail.crypt

import android.content.Context
import android.util.Log
import org.bouncycastle.openpgp.PGPPublicKeyRing

object PublicKeyProviders {
    private val providers = arrayOf(
        AutocryptPublicKeyProvider(),
        WKDPublicKeyProvider(),
        // KeyServerKeyProvider("url") ...
        // OpenKeychainKeyProvider()
        // etc
    )
    suspend fun getKeyRing(address: String, context: Context): PGPPublicKeyRing? {
        for (provider in providers)
            provider.getKeyRing(address, context)?.let { key ->
                Log.d("PublicKeyProviders", "Key provided by $provider")
                return key
            }
        return null
    }
}