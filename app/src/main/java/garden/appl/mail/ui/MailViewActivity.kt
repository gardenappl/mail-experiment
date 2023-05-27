package garden.appl.mail.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import garden.appl.mail.databinding.ActivityMailViewBinding
import garden.appl.mail.mail.MailAccount
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.Date
import java.util.Properties

private const val LOGGING_TAG = "MailViewActivity"

class MailViewActivity : AppCompatActivity(), CoroutineScope by MainScope()  {
    private lateinit var _binding: ActivityMailViewBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMailViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.composeButton.setOnClickListener {
            val account = MailAccount.getDefault(this)!!
            val props = Properties()
            props["mail.smtps.host"] = account.smtpAddress
            props["mail.smtps.port"] = account.smtpPort

            val session = Session.getInstance(props)
            // create a message
            val msg = MimeMessage(session)
            msg.setFrom(InternetAddress(account.originalAddress))
            val address = arrayOf(InternetAddress("yurii@mailbox.org"))
            msg.setRecipients(Message.RecipientType.TO, address)
            msg.subject = "Jakarta Mail APIs Multipart Test"
            msg.sentDate = Date()

            msg.setContent("Just testing things", "text/plain")

            launch(Dispatchers.IO) {

                try {
                    session.getTransport("smtps").use { transport ->
                        Log.d(LOGGING_TAG, "connecting...")
                        transport.connect(account.originalAddress, account.password)
                        Log.d(LOGGING_TAG, "sending...")
                        transport.sendMessage(msg, address)
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
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}