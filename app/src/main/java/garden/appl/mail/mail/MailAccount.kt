package garden.appl.mail.mail

import android.content.Context
import android.icu.text.IDNA
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import garden.appl.mail.MailDatabase
import garden.appl.mail.MailTypeConverters
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import java.lang.Exception
import java.lang.StringBuilder
import java.util.Date
import java.util.Properties

private const val ORIGINAL_ADDRESS = "origAddress"
private const val CANONICAL_ADDRESS = "canonAddress"
private const val PASSWORD = "password"
private const val IMAP_ADDRESS = "imapAddr"
private const val IMAP_PORT = "imapPort"
private const val SMTP_ADDRESS = "smtpAddr"
private const val SMTP_PORT = "smtpPort"
private const val JMAP_AUTHENTICATION = "jmapAuthentication"
private const val JMAP_SESSION_URL = "jmapSessionUrl"
private const val KEYRING_ARMORED = "keyringArmored"

data class MailAccount(
    val originalAddress: String,
    val password: String,
    val imapAddress: String?,
    val imapPort: Int?,
    val smtpAddress: String?,
    val smtpPort: Int?,
    val jmapAuthentication: String?,
    val jmapSessionUrl: String?,
    val keyRing: PGPSecretKeyRing
) {
    val canonAddress: String

    private lateinit var _session: Session

    val session: Session get() {
        return if (this::_session.isInitialized) {
            _session
        } else {
            val props = Properties()
            if (jmapAuthentication != null) {
                props["mail.store.protocol"] = "jmap"
                props["mail.jmap.auth"] = jmapAuthentication
                props["mail.jmap.session"] = jmapSessionUrl!!
            } else {
                props["mail.store.protocol"] = "smtps"
                props["mail.smtps.host"] = smtpAddress!!
                props["mail.smtps.port"] = smtpPort!!
            }

            val session = Session.getInstance(props)
            session.debug = true
            _session = session
            _session
        }
    }

    val publicKeyRing: PGPPublicKeyRing
    get() = PGPPublicKeyRing(keyRing.publicKeys.asSequence().toList())

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

    fun setAsCurrent(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit(commit = true) {
            putString(ORIGINAL_ADDRESS, originalAddress)
            putString(CANONICAL_ADDRESS, canonAddress)
            putString(PASSWORD, password)
            putString(IMAP_ADDRESS, imapAddress)
            putInt(IMAP_PORT, imapPort!!)
            putString(SMTP_ADDRESS, smtpAddress)
            putInt(SMTP_PORT, smtpPort!!)
            putString(KEYRING_ARMORED, PGPainless.asciiArmor(keyRing))
        }
    }

    constructor(
        originalAddress: String,
        password: String,
        imapAddress: String? = null,
        imapPort: Int? = null,
        smtpAddress: String? = null,
        smtpPort: Int? = null,
        jmapAuthentication: String? = null,
        jmapSessionUrl: String? = null,
    ) : this(
        originalAddress,
        password,
        imapAddress,
        imapPort,
        smtpAddress,
        smtpPort,
        jmapAuthentication,
        jmapSessionUrl,
        PGPainless.generateKeyRing().modernKeyRing(originalAddress)
    )

    suspend fun send(msg: MimeMessage, to: Array<InternetAddress>) {
        // create a message
        msg.setFrom(InternetAddress(originalAddress))
        msg.setRecipients(Message.RecipientType.TO, to)
        msg.sentDate = Date()

        withContext(Dispatchers.IO) {
            try {
                session.transport.use { transport ->
                    transport.connect(originalAddress, password)
                    transport.sendMessage(msg, to)
                }
            } catch (mex: MessagingException) {
                var ex: Exception? = mex
                do {
                    ex?.printStackTrace()
                    ex = (ex as? MessagingException)?.nextException
                } while (ex != null)
            }
        }
    }

    suspend fun connectToStore(): Store {
        val props = Properties()
        if (imapAddress != null) {
            props["mail.store.protocol"] = "imaps"
            props["mail.imaps.host"] = imapAddress
            props["mail.imaps.port"] = imapPort!!
        } else {
            props["mail.store.protocol"] = "jmap"
            props["mail.jmap.auth"] = jmapAuthentication!!
            props["mail.jmap.session"] = jmapSessionUrl!!
        }

        val session = Session.getInstance(props)
        return withContext(Dispatchers.IO) {
            val store = session.store
            store.connect(originalAddress, password)
            return@withContext store
        }
    }

    companion object {
        const val LOGGING_TAG = "MailAccount"

        fun getCurrent(context: Context): MailAccount? {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val originalAddress = prefs.getString(ORIGINAL_ADDRESS, "")
            if (originalAddress.isNullOrBlank())
                return null
            return MailAccount(
                originalAddress = originalAddress,
                password = prefs.getString(PASSWORD, "")!!,
                imapAddress = prefs.getString(IMAP_ADDRESS, "").run { if (this == "") null else this },
                imapPort = prefs.getInt(IMAP_PORT, 0).run { if (this == 0) null else this },
                smtpAddress = prefs.getString(SMTP_ADDRESS, "").run { if (this == "") null else this },
                smtpPort = prefs.getInt(SMTP_PORT, 0).run { if (this == 0) null else this },
                jmapAuthentication = prefs.getString(JMAP_AUTHENTICATION, "").run { if (this == "") null else this },
                jmapSessionUrl = prefs.getString(JMAP_SESSION_URL, "").run { if (this == "") null else this },
                keyRing = PGPainless.readKeyRing().secretKeyRing(
                    prefs.getString(KEYRING_ARMORED, "")!!)!!
            )
        }
    }
}