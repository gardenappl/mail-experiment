package garden.appl.mail.ui

import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatActivity
import garden.appl.mail.MailDatabase
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.R
import garden.appl.mail.databinding.FragmentMessageBinding
import garden.appl.mail.mail.MailAccount
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MessageReadActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        const val EXTRA_MESSAGE_LOCAL_ID = "message_local_id"
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
                binding.body.text = when {
                    mimeMessage.isMimeType("text/plain") -> mimeMessage.content as String
                    mimeMessage.isMimeType("multipart/*") -> {
                        val multipart = mimeMessage.content as MimeMultipart
                        var text = getString(R.string.cannot_display_message)
                        for (i in 0 until multipart.count) {
                            val part = multipart.getBodyPart(i)
                            if (part.isMimeType("text/plain")) {
                                text = part.content as String
                            }
                        }
                        text
                    }
                    else -> getString(R.string.unknown_msg_type, mimeMessage.contentType)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}