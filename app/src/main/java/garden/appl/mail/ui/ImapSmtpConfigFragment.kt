package garden.appl.mail.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.R
import garden.appl.mail.crypt.AutocryptSetupMessage
import garden.appl.mail.databinding.AutocryptNumericPasswordBinding
import garden.appl.mail.databinding.FragmentLoginConfigBinding
import garden.appl.mail.mail.MailAccount
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPException
import org.pgpainless.PGPainless
import org.pgpainless.util.Passphrase

private const val LOGGING_TAG = "ImapSmtpConfigFragment"

class ImapSmtpConfigFragment : Fragment(), CoroutineScope by MainScope() {
    private lateinit var binding: FragmentLoginConfigBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentLoginConfigBinding.inflate(inflater, container, false)

        val watcher = LoginButtonWatcher(binding)
        for (item in arrayOf(
            binding.textImapAddress,
            binding.textImapPort,
            binding.textSmtpAddress,
            binding.textSmtpPort
        )) {
            item.addTextChangedListener(watcher)
        }

        val loginActivity = activity as LoginActivity

        val uiElements = listOf(
            binding.textImapAddress,
            binding.textImapPort,
            binding.textSmtpAddress,
            binding.textSmtpPort,
            binding.buttonLogin
        )
        if (loginActivity.address.endsWith("fastmail.com")) {
            for (element in uiElements)
                element.isEnabled = false

            val jmapAccount = MailAccount(
                originalAddress = loginActivity.address,
                password = loginActivity.password,
                jmapAuthentication = "Bearer ${loginActivity.password}",
                jmapSessionUrl = "https://api.fastmail.com/jmap/session"
            )
//            account.setAsCurrent(requireContext())
            launch(Dispatchers.IO) {
                try {
                    AutocryptDialog.setup(jmapAccount, requireContext())
                } catch (e: Exception) {
                    Log.e(LOGGING_TAG, "Failed to log in", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, R.string.login_fail, Toast.LENGTH_LONG).show()
                        loginActivity.viewPager.currentItem = 0
                    }
                    return@launch
                }
            }
        } else {
            for (element in uiElements)
                element.isEnabled = true

            binding.buttonLogin.setOnClickListener {
                val account = MailAccount(
                    originalAddress = loginActivity.address,
                    password = loginActivity.password,
                    imapAddress = binding.textImapAddress.text.toString(),
                    imapPort = Integer.parseInt(binding.textImapPort.text.toString()),
                    smtpAddress = binding.textSmtpAddress.text.toString(),
                    smtpPort = Integer.parseInt(binding.textSmtpPort.text.toString())
                )
//            account.setAsCurrent(requireContext())
                launch(Dispatchers.IO) {
                    try {
                        AutocryptDialog.setup(account, requireContext())
                    } catch (e: Exception) {
                        Log.e(LOGGING_TAG, "Failed to log in", e)
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, R.string.login_fail, Toast.LENGTH_LONG).show()
                            loginActivity.viewPager.currentItem = 0
                        }
                        return@launch
                    }
                }
            }
        }
        return binding.root
    }

    private inner class LoginButtonWatcher(private val binding: FragmentLoginConfigBinding) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            when {
                binding.textImapAddress.text.isNullOrEmpty() ||
                        binding.textImapPort.text.isNullOrEmpty() ||
                        binding.textSmtpAddress.text.isNullOrEmpty() ||
                        binding.textSmtpPort.text.isNullOrEmpty() ->
                    binding.buttonLogin.isEnabled = false
                else ->
                    binding.buttonLogin.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}