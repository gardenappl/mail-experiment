package garden.appl.mail.ui

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import garden.appl.mail.MailApp
import garden.appl.mail.R
import garden.appl.mail.databinding.FragmentLoginBinding
import garden.appl.mail.mail.MailAccount

private const val LOGGING_TAG = "LoginFragment"

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Log.d(LOGGING_TAG, "onCreate")
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        binding.textEmail.addTextChangedListener {
            //Reset email error
            if (binding.layoutEmail.error != null) {
                if (Patterns.EMAIL_ADDRESS.matcher(it).matches())
                    binding.layoutEmail.error = null
            }
            (activity as LoginActivity).address = it.toString()
            updateLogInButtonAvailability()
        }
        binding.textPassword.addTextChangedListener {
            (activity as LoginActivity).password = it.toString()
            updateLogInButtonAvailability()
        }

        binding.buttonLogin.setOnClickListener {
            (activity as LoginActivity).viewPager.currentItem++
        }
        return binding.root
    }

    private fun updateLogInButtonAvailability() {
        when {
            binding.layoutEmail.error != null ||
            binding.textEmail.text.isNullOrEmpty() ->
                binding.buttonLogin.isEnabled = false
            else ->
                binding.buttonLogin.isEnabled = true
        }
    }
}