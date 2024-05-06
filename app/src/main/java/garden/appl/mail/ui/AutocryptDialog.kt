package garden.appl.mail.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.R
import garden.appl.mail.crypt.AutocryptSetupMessage
import garden.appl.mail.databinding.AutocryptNumericPasswordBinding
import garden.appl.mail.mail.MailAccount
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPException
import org.pgpainless.PGPainless
import org.pgpainless.util.Passphrase

object AutocryptDialog {
    private const val LOGGING_TAG = "AutocryptDialog"

    suspend fun setup(account: MailAccount, context: Context) {
        account.connectToStore().use { store ->
            MailTypeConverters.toDatabase(store.defaultFolder)
                .syncFoldersRecursive(context, store)
        }
        val setupMessage = AutocryptSetupMessage.findExisting(account)
        Log.d(LOGGING_TAG, "Found setup msg? $setupMessage")
        if (setupMessage != null) {
            val dialogBuilder = AlertDialog.Builder(context).apply {
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
                        bootstrappedAccount.setAsCurrent(context)

                        context.startActivity(
                            Intent(context, MailViewActivity::class.java)
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
            setupAutocrypt(account, context)
        }
    }

    private suspend fun setupAutocrypt(account: MailAccount, context: Context) {
        val (message, passphrase) =
            AutocryptSetupMessage.generate(account, context)
        val dialogBuilder = AlertDialog.Builder(context).apply {
            setTitle(R.string.autocrypt_setup_dialog_title)
            setMessage(context.getString(R.string.autocrypt_setup_dialog, String(passphrase.chars!!)))
            setPositiveButton(android.R.string.ok) { _, _ ->
                runBlocking {
                    account.send(message,
                        arrayOf(InternetAddress(account.originalAddress)))
                    account.setAsCurrent(context)
                    context.startActivity(
                        Intent(context, MailViewActivity::class.java)
                            .putExtra(MailViewActivity.EXTRA_FOLDER_FULL_NAME, "INBOX")
                            .putExtra(MailViewActivity.EXTRA_FIRST_VISIT, true)
                    )
                }
            }
            setCancelable(false)
        }
        passphrase.clear()

        withContext(Dispatchers.Main) {
            dialogBuilder.show()
        }
    }
}