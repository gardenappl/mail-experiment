package garden.appl.mail.ui

import android.content.ContentResolver.MimeTypeInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import garden.appl.mail.MailDatabase
import garden.appl.mail.R
import garden.appl.mail.crypt.PublicKeyProviders
import garden.appl.mail.databinding.FragmentSelectRecipientBinding
import jakarta.mail.internet.MimeUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SelectRecipientActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var _binding: FragmentSelectRecipientBinding
    val binding get() = _binding

    companion object {
        const val LOGGING_TAG = "SelectRecipientActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = FragmentSelectRecipientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textInput.addTextChangedListener { text ->
            launch {
                Log.d(LOGGING_TAG, "Text change: $text")
                val address = text.toString()
                val keyRing = PublicKeyProviders.getKeyRing(address, this@SelectRecipientActivity)
                if (address != binding.textInput.text.toString()) {
                    return@launch
                }

                launch(Dispatchers.Main) {
                    if (keyRing != null) {
                        binding.encryptionAvailable.text =
                            getString(R.string.encryption_available, text.toString())
                        binding.encryptionAvailable.isVisible = true
                        binding.writePrivateMessage.isEnabled = true
                    } else {
                        Log.d(LOGGING_TAG, "No key for $address")
                        binding.encryptionAvailable.isVisible = false
                        binding.writePrivateMessage.isEnabled = false
                    }
                }
            }
        }
        binding.writeMessage.setOnClickListener {
            startActivity(Intent(this, MessageWriteActivity::class.java)
                .putExtra(MessageWriteActivity.EXTRA_RECIPIENTS_TO, binding.textInput.text.toString()))
        }
        binding.writePrivateMessage.setOnClickListener {
            startActivity(Intent(this, MessageWriteActivity::class.java)
                .putExtra(MessageWriteActivity.EXTRA_RECIPIENTS_TO, binding.textInput.text.toString())
                .putExtra(MessageWriteActivity.EXTRA_DO_ENCRYPT, true))
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}