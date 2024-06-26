package garden.appl.mail.crypt

import android.content.Context
import android.text.Html
import android.text.TextUtils
import android.util.Log
import garden.appl.mail.R
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.mail.MailAccount
import garden.appl.mail.mail.MailMessage
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.search.HeaderTerm
import jakarta.mail.search.SearchTerm
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.util.io.Streams
import org.eclipse.angus.mail.imap.SortTerm
import org.eclipse.angus.mail.util.BASE64DecoderStream
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Scanner

object AutocryptSetupMessage {
    private const val HEADER_KEY = "Autocrypt-Setup-Message"
    private const val LOGGING_TAG = "AutocryptSetupMsg"

    suspend fun findExisting(account: MailAccount): MailMessage? {
        account.connectToStore().use { store ->
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            inbox.use { folder ->
                val messages = folder.search(
                    HeaderTerm(HEADER_KEY, "v1")
                ).map { message ->
                    MailTypeConverters.toDatabase(message as MimeMessage)
                }

                if (messages.isEmpty())
                    return null
                else
                    return messages.maxBy(MailMessage::effectiveDate)
            }
        }
    }

    fun bootstrapFrom(account: MailAccount, mailMessage: MailMessage, passphrase: Passphrase): PGPSecretKeyRing {
        Log.d(LOGGING_TAG, "BEFORE: ${PGPainless.asciiArmor(account.keyRing)}")

        val message = MailTypeConverters.fromDatabase(mailMessage, account.session)
        val multipart = message.content as MimeMultipart

        for (i in 0 until multipart.count) {
            var part = multipart.getBodyPart(i)
            if (part.isMimeType("multipart/*")) {
                val subMultipart = part.content as MimeMultipart
                for (i in 0 until subMultipart.count) {
                    val subPart = subMultipart.getBodyPart(i)
                    if (subPart.isMimeType("application/autocrypt-setup")) {
                        part = subPart
                        break
                    }
                }
            }
            if (!part.isMimeType("application/autocrypt-setup"))
                continue

            val sb = StringBuilder()
            val inputStream = if (part.content is ByteArrayInputStream) {
                Log.d(LOGGING_TAG, "BAI stream")
                part.content as ByteArrayInputStream
            } else {
                Log.d(LOGGING_TAG, "Base64 stream")
                part.content as BASE64DecoderStream
            }
            val scanner = Scanner(inputStream)
            var isPGPMessage = false
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                if (line == "-----BEGIN PGP MESSAGE-----")
                    isPGPMessage = true
                if (isPGPMessage)
                    sb.appendLine(line)
                if (line == "-----END PGP MESSAGE-----")
                    isPGPMessage = false
            }
            val filtered = sb.toString()
            Log.d(LOGGING_TAG, "FILTERED: $filtered")

            val payload = ByteArrayOutputStream().use { decryptedStream ->
                (filtered.byteInputStream()).use { inputStream ->
                    val decryptionStream = PGPainless.decryptAndOrVerify()
                        .onInputStream(inputStream)
                        .withOptions(
                            ConsumerOptions()
                                .addDecryptionPassphrase(passphrase)
//                                .forceNonOpenPgpData()
                        )
                    decryptionStream.use {
                        Streams.pipeAll(it, decryptedStream)
                    }
                    Log.d(LOGGING_TAG, "Algo: ${decryptionStream.metadata.encryptionAlgorithm}")
                }

                return@use decryptedStream.toByteArray()
            }
            val asciiArmoredPayload = String(payload)
            Log.d(LOGGING_TAG, "AFTER: $asciiArmoredPayload")
            return PGPainless.readKeyRing().secretKeyRing(asciiArmoredPayload)!!
        }

        throw IllegalArgumentException()
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

        // String are a bit less secure than char[] or Passphrase
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
                setContent(
                    """
                        <html>
                        <body><p>${Html.escapeHtml(context.getString(R.string.autocrypt_setup_attachment))
                        .lineSequence().joinToString(separator = " ") }</p>
                        <pre>
                        $pgpMessage
                        </pre></body>
                        </html>
                        """.lines().joinToString(separator = "\n", transform = String::trim),
                    "application/autocrypt-setup"
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