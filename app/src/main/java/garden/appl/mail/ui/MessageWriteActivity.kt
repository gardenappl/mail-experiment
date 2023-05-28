package garden.appl.mail.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import garden.appl.mail.R
import garden.appl.mail.databinding.FragmentMessageBinding
import garden.appl.mail.databinding.FragmentMessageWriteBinding
import garden.appl.mail.mail.MailAccount
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MessageWriteActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    companion object {
        const val EXTRA_RECIPIENTS_TO = "to"
        const val EXTRA_DO_ENCRYPT = "encrypt"
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
            message.setContent(binding.textBody.text.toString(), "text/plain")
            launch {
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