package garden.appl.mail.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import garden.appl.mail.databinding.FragmentLoginConfigBinding
import garden.appl.mail.mail.MailAccount

private const val LOGGING_TAG = "LoginConfigFrag"

class LoginConfigFragment : Fragment() {
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
            MailAccount(
                originalAddress = loginActivity.address,
                password = loginActivity.password,
                imapAddress = binding.textImapAddress.text.toString(),
                imapPort = Integer.parseInt(binding.textImapPort.text.toString()),
                smtpAddress = binding.textSmtpAddress.text.toString(),
                smtpPort = Integer.parseInt(binding.textSmtpPort.text.toString())
            ).setAsCurrent(requireContext())

            startActivity(Intent(requireContext(), MailViewActivity::class.java)
                .putExtra(MailViewActivity.EXTRA_FOLDER_FULL_NAME, "INBOX"))
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
}