package garden.appl.mail.ui

import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import garden.appl.mail.MailDatabase
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.R
import garden.appl.mail.crypt.AutocryptHeader
import garden.appl.mail.databinding.FragmentMessageBinding
import garden.appl.mail.mail.MailAccount
import jakarta.mail.Part
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimePart
import jakarta.mail.internet.MimeUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MessageReadActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        const val EXTRA_MESSAGE_LOCAL_ID = "message_local_id"

        const val LOGGING_TAG = "MessageReadActivity"
    }

    private lateinit var _binding: FragmentMessageBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = FragmentMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        val localID = intent.getIntExtra(EXTRA_MESSAGE_LOCAL_ID, -1)

        launch(Dispatchers.IO) {
            val message = MailDatabase.getDatabase(this@MessageReadActivity)
                .messageDao.getMessage(localID)!!
            Log.d(LOGGING_TAG, "autocrypt: ${message.autocryptHeader}")
            message.autocryptHeader?.run {
                val header = AutocryptHeader.parseHeaderValue(this)
                Log.d(LOGGING_TAG, header.keyRing.publicKey.userIDs.next().toString())
            }
            Log.d(LOGGING_TAG, message.toString())


            val session = MailAccount.getCurrent(this@MessageReadActivity)!!.session
            val mimeMessage = MailTypeConverters.fromDatabase(message, session)

            launch(Dispatchers.Main) {
                binding.date.text = DateUtils.getRelativeDateTimeString(
                    this@MessageReadActivity,
                    mimeMessage.sentDate.time,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                binding.subject.text = mimeMessage.subject
                binding.from.text = getString(R.string.from, message.from)

                displayMessageBody(mimeMessage, message.from)
            }
        }
    }

    private suspend fun displayMessageBody(messagePart: Part, from: String) {
        Log.d(LOGGING_TAG, "DISPLAYING ${messagePart.contentType}")
        if (messagePart.contentType.contains(Regex(""";\s*protected-headers="v1"""", RegexOption.IGNORE_CASE))) {
            binding.subject.text = MimeUtility.decodeWord(messagePart.getHeader("Subject").first())
        }
        when {
            messagePart.isMimeType("text/plain") -> {
                val type = messagePart.contentType
                if (type.contains(Regex(""";\s*format=flowed""", RegexOption.IGNORE_CASE))) {
                    val stringBuilder = StringBuilder()

                    val deleteSpace =
                        type.contains(Regex(""";\s*DelSp=Yes""", RegexOption.IGNORE_CASE))

                    for (line in (messagePart.content as String).lineSequence()) {
                        if (line.endsWith(' ')) {
                            if (deleteSpace)
                                stringBuilder.append(line.removeSuffix(" "))
                            else
                                stringBuilder.append(line)
                        } else {
                            stringBuilder.append(line)
                            stringBuilder.append('\n')
                        }
                    }

                    binding.wrapper.visibility = View.GONE
                    binding.body.visibility = View.VISIBLE
                    binding.body.text = stringBuilder.toString()
                } else {
                    binding.wrappedBody.text = messagePart.content as String
                }
            }

            messagePart.isMimeType("multipart/encrypted") -> run {
                val multipart = messagePart.content as MimeMultipart
                var isPGPv1 = false
                for (i in 0 until multipart.count) {
                    val part = multipart.getBodyPart(i)
                    if (part.isMimeType("application/pgp-encrypted")) {
                        if (part.getHeader("Version")?.contains("1") == true) {
                            isPGPv1 = true
                            break
                        }
                        val content = String((part.content as ByteArrayInputStream).readBytes())
                        if (content.contains(Regex("""Version:\s*1"""))) {
                            isPGPv1 = true
                            break
                        }
                    }
                }
                if (!isPGPv1) {
                    binding.wrappedBody.text = getString(R.string.unknown_encrypt)
                    return
                }

                val account = MailAccount.getCurrent(this@MessageReadActivity)!!
                val privateKey = account.keyRing

                val lastMessage = withContext(Dispatchers.IO) {
                    val db = MailDatabase.getDatabase(this@MessageReadActivity)

                    return@withContext db.messageDao.getMostRecentMessageFrom(from)
                }
                val lastAutocryptHeader = lastMessage?.autocryptHeader?.let { value ->
                    AutocryptHeader.parseHeaderValue(value)
                }

                for (i in 0 until multipart.count) {
                    val part = multipart.getBodyPart(i)
                    if (part.isMimeType("application/octet-stream")) {
//                        val encrypted = String((part.content as ByteArrayInputStream).readBytes())

//                        Log.d(
//                            LOGGING_TAG, "Encrypted message is : ${
//                                String((part.content as ByteArrayInputStream).readBytes())
//                            }"
//                        )

                        return@run ByteArrayOutputStream().use { decryptedStream ->
//                            encrypted.byteInputStream().use { encryptedStream ->
                            (part.content as ByteArrayInputStream).use { encryptedStream ->
                                val decryptionStream = PGPainless.decryptAndOrVerify()
                                    .onInputStream(encryptedStream)
                                    .withOptions(
                                        ConsumerOptions.get()
                                            .addVerificationCert(lastAutocryptHeader?.keyRing)
                                            .addDecryptionKey(privateKey)
                                    )

                                decryptionStream.use {
                                    Streams.pipeAll(it, decryptedStream)
                                }
                                val result = decryptionStream.metadata
                                lastAutocryptHeader?.let {
                                    val isVerified = result.isVerifiedSignedBy(it.keyRing)
                                    Log.d(LOGGING_TAG, "Is verified: $isVerified")
                                }
                                Log.d(LOGGING_TAG, "was encrypted?: ${result.isEncrypted}")
                            }
                            val decryptedMessage = decryptedStream.toByteArray()
                            displayMessageBody(MimeMessage(
                                account.session,
                                ByteArrayInputStream(decryptedMessage)
                            ), from)
                            return
                        }

                    }
                }


                binding.wrappedBody.text = getString(R.string.wrong_encrypt)
            }

            messagePart.isMimeType("multipart/*") -> {
                val multipart = messagePart.content as MimeMultipart
                for (i in 0 until multipart.count) {
                    val part = multipart.getBodyPart(i)
                    if (part.isMimeType("text/plain")) {
                        displayMessageBody(part, from)
                        return
                    }
                }
                binding.wrappedBody.text = getString(R.string.cannot_display_message)
            }

            else -> {
                binding.wrappedBody.text = getString(R.string.unknown_msg_type, messagePart.contentType)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}