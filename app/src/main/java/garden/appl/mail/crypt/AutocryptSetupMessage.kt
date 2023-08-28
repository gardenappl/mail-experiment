package garden.appl.mail.crypt

import android.content.Context
import android.icu.text.CaseMap.Fold
import android.text.TextUtils
import garden.appl.mail.R
import garden.appl.mail.mail.MailAccount
import garden.appl.mail.mail.MailMessage
import jakarta.mail.Address
import jakarta.mail.BodyPart
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.util.ArmoredOutputStreamFactory
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.security.SecureRandom

object AutocryptSetupMessage {
    private const val HEADER_KEY = "Autocrypt-Setup-Message"

    suspend fun findExisting(account: MailAccount): Message? {
        account.connectToStore().use { store ->
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            inbox.use {  folder ->

                val messages = inbox.messages
                folder.fetch(messages, FetchProfile().apply {
                    add(HEADER_KEY)
                })
                for (message in messages) {
                    if (message.getHeader(HEADER_KEY)?.contains("v1") == true
//                    && message.from.contains(InternetAddress(account.originalAddress))
//                    && message.getRecipients(Message.RecipientType.TO).contains(InternetAddress(account.originalAddress))
                    ) {
                        return message
                    }
                }
            }
        }
        return null
    }

    fun generate(account: MailAccount, context: Context): Pair<MimeMessage, Passphrase> {
        val random = SecureRandom()
        val password = Passphrase(CharArray(44) { i ->
            // ensure the first digit is not 0, for
            // the sake of Passphrase-Begin compatibility
            if (i == 0)
                Char('1'.code + random.nextInt(9))
            else if (i % 5 == 4)
                '-'
            else
                Char('0'.code + random.nextInt(10))
        })

        val pgpMessage = ByteArrayOutputStream().use { encryptedStream ->
            PGPainless.asciiArmor(account.keyRing).byteInputStream().use { sourceStream ->
                PGPainless.encryptAndOrSign()
                    .onOutputStream(encryptedStream)
                    .withOptions(ProducerOptions.encrypt(
                        EncryptionOptions.encryptDataAtRest().apply {
                            this.addPassphrase(password)
                        }
                    ).apply {
                        isAsciiArmor = true
                    }).use { stream ->
                        Streams.pipeAll(sourceStream, stream)
                    }
            }

            return@use String(encryptedStream.toByteArray())
        }

        //Warning: not secure!
        // https://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords/8881376#8881376
//        val passwordString = password.joinToString(
//            separator = "-",
//            transform = { int -> int.toString().padStart(4, '0') }
//        )

        val message = MimeMessage(account.session)
        message.setFrom(account.originalAddress)
        message.setRecipients(Message.RecipientType.TO, account.originalAddress)
        message.setHeader(HEADER_KEY, "v1")
        message.subject = context.getString(R.string.autocrypt_setup_subject)

        message.setContent(MimeMultipart().apply {
            addBodyPart(MimeBodyPart().apply {
                setContent(context.getString(R.string.autocrypt_setup), "text/plain")
            })
            addBodyPart(MimeBodyPart().apply {
                val privateKeyDump = PGPainless.asciiArmor(account.keyRing)
                setContent(
                    """
                        <html>
                            <body>
                                <p>
                                ${TextUtils.htmlEncode(context.getString(R.string.autocrypt_setup_attachment))}
                                </p>
                                <pre>
                                $pgpMessage
                                </pre>
                            </body>
                        </html>""".trimIndent(), "application/autocrypt-setup"
                )
                setHeader(
                    "Content-Disposition",
                    "attachment; filename=\"autocrypt-setup-message.html\""
                )
            })
        })
        return Pair(message, password)
    }
}