package garden.appl.mail.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import garden.appl.mail.MailDatabase
import garden.appl.mail.R
import garden.appl.mail.crypt.AutocryptHeader
import garden.appl.mail.databinding.FragmentMessageWriteBinding
import garden.appl.mail.mail.MailAccount
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.encryption_signing.SigningOptions
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.ByteArrayOutputStream

class MessageWriteActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        const val EXTRA_RECIPIENTS_TO = "to"
        const val EXTRA_DO_ENCRYPT = "encrypt"

        const val LOGGING_TAG = "MsgWriteActivity"
    }

    private lateinit var _binding: FragmentMessageWriteBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = FragmentMessageWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toAddress = intent.getStringExtra(EXTRA_RECIPIENTS_TO)
        binding.to.text = getString(R.string.to, toAddress)
        binding.sendButton.setOnClickListener {
            val account = MailAccount.getCurrent(this)!!
            val message = MimeMessage(account.session)
            message.subject = binding.textSubject.text.toString()
            val textBody = binding.textBody.text.toString()

            launch(Dispatchers.IO) {
                if (intent.getBooleanExtra(EXTRA_DO_ENCRYPT, false)) {
                    val db = MailDatabase.getDatabase(this@MessageWriteActivity)
                    val lastMessage = db.messageDao.getMostRecentMessageFrom(toAddress!!)!!

                    val autocryptHeader = AutocryptHeader(lastMessage.autocryptHeader!!)
                    val outputStream = ByteArrayOutputStream()

                    val encryptionStream = PGPainless.encryptAndOrSign()
                        .onOutputStream(outputStream)
                        .withOptions(ProducerOptions.signAndEncrypt(
                            EncryptionOptions.encryptCommunications()
                                .addRecipient(autocryptHeader.keyRing),
                            SigningOptions.get().addSignature(
                                SecretKeyRingProtector.unprotectedKeys(),
                                account.keyRing
                            )
                        ).apply {
                            isAsciiArmor = true
                        })

                    Streams.pipeAll(textBody.byteInputStream(), encryptionStream)
                    encryptionStream.close()

                    val outputString = String(outputStream.toByteArray())
                    Log.d(LOGGING_TAG, "out str: $outputString")
                    val result = encryptionStream.result
                    Log.d(LOGGING_TAG, "result: ${result.encryptionAlgorithm}")

                    message.setContent(MimeMultipart("encrypted; protocol=\"application/pgp-encrypted\"",
                        MimeBodyPart().apply {
                            setContent("Version: 1", "application/pgp-encrypted")
                        },
                        MimeBodyPart().apply {
                            setContent(outputString, "application/octet-stream")
                        }
                    ))
                } else {
                    message.setContent(textBody, "text/plain")
                }

                account.send(message, InternetAddress.parse(toAddress))
                this@MessageWriteActivity.finishAndRemoveTask()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}