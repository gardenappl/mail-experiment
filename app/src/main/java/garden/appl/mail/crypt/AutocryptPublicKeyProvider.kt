package garden.appl.mail.crypt

import android.content.Context
import garden.appl.mail.MailDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPPublicKeyRing

class AutocryptPublicKeyProvider : PublicKeyProvider() {
    override suspend fun getKeyRing(address: String, context: Context): PGPPublicKeyRing? {
        val db = MailDatabase.getDatabase(context)

        val lastMessage = db.messageDao.getMostRecentMessageFrom(address)
        return lastMessage?.autocryptHeader?.let { value ->
            AutocryptHeader.parseHeaderValue(value)
        }?.keyRing
    }
}