package garden.appl.mail.ui

import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import garden.appl.mail.MailApp
import garden.appl.mail.R
import garden.appl.mail.databinding.LoginActivityBinding
import garden.appl.mail.mail.MailAccount

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textEmail.addTextChangedListener {
            //Reset email error
            if (binding.layoutEmail.error != null) {
                if (Patterns.EMAIL_ADDRESS.matcher(binding.textEmail.text).matches())
                    binding.layoutEmail.error = null
            }
        }

        binding.buttonLogin.setOnClickListener {
            if (Patterns.EMAIL_ADDRESS.matcher(binding.textEmail.text).matches()) {
                MailApp.account = MailAccount(
                    binding.textEmail.text.toString(),
                    binding.textPassword.text.toString()
                )
                binding.layoutEmail.error = null
            } else {
                binding.layoutEmail.error = getString(R.string.login_email_invalid)
            }
        }
    }
}