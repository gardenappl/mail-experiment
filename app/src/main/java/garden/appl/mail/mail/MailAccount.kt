package garden.appl.mail.mail

import android.content.Context
import android.icu.text.IDNA
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.lang.StringBuilder

private const val ORIGINAL_ADDRESS = "origAddress"
private const val CANONICAL_ADDRESS = "canonAddress"
private const val PASSWORD = "password"
private const val IMAP_ADDRESS = "imapAddr"
private const val IMAP_PORT = "imapPort"
private const val SMTP_ADDRESS = "smtpAddr"
private const val SMTP_PORT = "smtpPort"

data class MailAccount(
    private val originalAddress: String,
    private val password: String,
    private val imapAddress: String,
    private val imapPort: Int,
    private val smtpAddress: String,
    private val smtpPort: Int
) {
    val canonAddress: String

    init {
        // Canonicalize according to Autocrypt Level 1
        val (addressLocal, addressHost) = originalAddress.split('@')

        val stringBuilder = StringBuilder()
        val info = IDNA.Info()
        IDNA.getUTS46Instance(IDNA.DEFAULT).nameToASCII(addressHost, stringBuilder, info)
        if (info.hasErrors())
            throw IllegalArgumentException("Bad address $originalAddress, errors: ${info.errors.joinToString()}")

        canonAddress = addressLocal.lowercase() + '@' + stringBuilder
    }

    fun setAsDefault(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            putString(ORIGINAL_ADDRESS, originalAddress)
            putString(CANONICAL_ADDRESS, canonAddress)
            putString(PASSWORD, password)
            putString(IMAP_ADDRESS, imapAddress)
            putInt(IMAP_PORT, imapPort)
            putString(SMTP_ADDRESS, smtpAddress)
            putInt(SMTP_PORT, smtpPort)
        }
    }

    companion object {
        fun getDefault(context: Context): MailAccount? {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.getString(ORIGINAL_ADDRESS, "").isNullOrBlank())
                return null
            return MailAccount(
                originalAddress = prefs.getString(ORIGINAL_ADDRESS, "")!!,
                password = prefs.getString(PASSWORD, "")!!,
                imapAddress = prefs.getString(IMAP_ADDRESS, "")!!,
                imapPort = prefs.getInt(IMAP_PORT, 0),
                smtpAddress = prefs.getString(SMTP_ADDRESS, "")!!,
                smtpPort = prefs.getInt(SMTP_PORT, 0)
            )
        }
    }
}