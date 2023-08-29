package garden.appl.mail.ui

import android.content.Intent
import android.icu.text.CaseMap.Fold
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import garden.appl.mail.MailDatabase
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.R
import garden.appl.mail.crypt.AutocryptSetupMessage
import garden.appl.mail.databinding.FragmentLoginConfigBinding
import garden.appl.mail.mail.MailAccount
import jakarta.mail.Folder
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val LOGGING_TAG = "LoginConfigFrag"

class LoginConfigFragment : Fragment(), CoroutineScope by MainScope() {
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
        binding.buttonLogin.setOnClickListener {
            val account = MailAccount(
                originalAddress = loginActivity.address,
                password = loginActivity.password,
                imapAddress = binding.textImapAddress.text.toString(),
                imapPort = Integer.parseInt(binding.textImapPort.text.toString()),
                smtpAddress = binding.textSmtpAddress.text.toString(),
                smtpPort = Integer.parseInt(binding.textSmtpPort.text.toString())
            )
            account.setAsCurrent(requireContext())
            launch(Dispatchers.IO) {
                try {
                    account.connectToStore().use { store ->
                        MailTypeConverters.toDatabase(store.defaultFolder)
                            .syncFoldersRecursive(requireContext(), store)
                        MailTypeConverters.toDatabase(store.getFolder("INBOX"))
                            .refreshDatabaseMessages(requireContext(), store)
                    }
                } catch (e: Exception) {
                    Log.e(LOGGING_TAG, "Failed to log in", e)
                    Toast.makeText(context, R.string.login_fail, Toast.LENGTH_LONG).show()
                    loginActivity.viewPager.currentItem = 0
                    return@launch
                }
                val found = AutocryptSetupMessage.findExisting(account)
                Log.d(LOGGING_TAG, "Found setup msg? $found")
                if (found == null) {
                    val (message, passphrase) =
                        AutocryptSetupMessage.generate(account, requireContext())
                    val dialogBuilder = AlertDialog.Builder(requireContext()).apply {
                        setTitle(R.string.autocrypt_setup_dialog_title)
                        setMessage(getString(R.string.autocrypt_setup_dialog, String(passphrase.chars!!)))
                        setPositiveButton(android.R.string.ok) { _, _ ->
                            Log.d(LOGGING_TAG, "Just gonna send it?")
                            runBlocking {
                                account.send(message,
                                    arrayOf(InternetAddress(account.originalAddress)))
                            }
                        }
                        setNegativeButton(android.R.string.cancel) { _, _ ->
                            // nothing
                        }
                    }
                    passphrase.clear()

                    withContext(Dispatchers.Main) {
                        dialogBuilder.show()
                    }
                }

                withContext(Dispatchers.Main) {
                    startActivity(
                        Intent(requireContext(), MailViewActivity::class.java)
                            .putExtra(MailViewActivity.EXTRA_FOLDER_FULL_NAME, "INBOX")
                    )
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