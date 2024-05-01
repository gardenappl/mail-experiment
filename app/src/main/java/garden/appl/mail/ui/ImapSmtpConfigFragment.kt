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
                    account.connectToStore().use { store ->
                        MailTypeConverters.toDatabase(store.defaultFolder)
                            .syncFoldersRecursive(requireContext(), store)
//                        MailTypeConverters.toDatabase(store.getFolder("INBOX"))
//                            .refreshDatabaseMessages(requireContext(), store)
                    }
                } catch (e: Exception) {
                    Log.e(LOGGING_TAG, "Failed to log in", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, R.string.login_fail, Toast.LENGTH_LONG).show()
                        loginActivity.viewPager.currentItem = 0
                    }
                    return@launch
                }
                val setupMessage = AutocryptSetupMessage.findExisting(account)
                Log.d(LOGGING_TAG, "Found setup msg? $setupMessage")
                if (setupMessage != null) {
                    val dialogBuilder = AlertDialog.Builder(requireContext()).apply {
                        setTitle(R.string.autocrypt_prompt_passphrase_title)
                        setMessage(R.string.autocrypt_prompt_passphrase)
                        val passwordView = AutocryptNumericPasswordBinding.inflate(
                            LayoutInflater.from(this.context)
                        )
                        val pinInputs = arrayOf(
                            passwordView.pin1,
                            passwordView.pin2,
                            passwordView.pin3,
                            passwordView.pin4,
                            passwordView.pin5,
                            passwordView.pin6,
                            passwordView.pin7,
                            passwordView.pin8,
                            passwordView.pin9
                        )
                        for (i in pinInputs.indices) {
                            val pin = pinInputs[i]
                            pin.addTextChangedListener { text ->
                                if (text?.length == 4) {
                                    if (i < pinInputs.size - 1)
                                        pinInputs[i + 1].requestFocus()
                                }
                            }
                        }
                        setView(passwordView.root)
                        setPositiveButton(android.R.string.ok) { _, _ ->
                            try {
                                val passChars = CharArray(44) { i ->
                                    when {
                                        i % 5 == 4 -> '-'
                                        else -> pinInputs[i / 5].text[i % 5]
                                    }
                                }
                                val passphrase = Passphrase(passChars)
                                Log.d(LOGGING_TAG, String(passphrase.chars!!))
                                val keyRing = try {
                                    AutocryptSetupMessage.bootstrapFrom(account, setupMessage,
                                        passphrase)
                                } catch (e: Exception) {
                                    throw e
                                } finally {
                                    passphrase.clear()
                                }

                                Log.d(LOGGING_TAG, "Key before: ${PGPainless.asciiArmor(account.keyRing)}")
                                Log.d(LOGGING_TAG, "Key after: ${PGPainless.asciiArmor(keyRing)}")
                                val bootstrappedAccount = account.copy(
                                    keyRing = keyRing
                                )
                                bootstrappedAccount.setAsCurrent(requireContext())

                                startActivity(
                                    Intent(requireContext(), MailViewActivity::class.java)
                                        .putExtra(MailViewActivity.EXTRA_FOLDER_FULL_NAME, "INBOX")
                                        .putExtra(MailViewActivity.EXTRA_FIRST_VISIT, true)
                                )
                            } catch (e: PGPException) {
                                Toast.makeText(context, R.string.autocrypt_prompt_passphrase_error, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        dialogBuilder.show()
                    }
                } else {
                    setupAutocrypt(account)
                }
            }
        }
        return binding.root
    }

    private suspend fun setupAutocrypt(account: MailAccount) {
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
                    account.setAsCurrent(context)
                    startActivity(
                        Intent(requireContext(), MailViewActivity::class.java)
                            .putExtra(MailViewActivity.EXTRA_FOLDER_FULL_NAME, "INBOX")
                            .putExtra(MailViewActivity.EXTRA_FIRST_VISIT, true)
                    )
                }
            }
            setCancelable(false)
//            setNegativeButton(android.R.string.cancel) { _, _ ->
//                // nothing
//            }
        }
        passphrase.clear()

        withContext(Dispatchers.Main) {
            dialogBuilder.show()
        }
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