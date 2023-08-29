package garden.appl.mail.crypt

import android.content.Context
import org.bouncycastle.openpgp.PGPPublicKeyRing

abstract class PublicKeyProvider {
    abstract suspend fun getKeyRing(address: String, context: Context): PGPPublicKeyRing?
}