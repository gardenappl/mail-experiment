package garden.appl.mail.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import garden.appl.mail.mail.MailAccount

class StartActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()

        val account = MailAccount.getDefault(this)
        if (account == null)
            startActivity(Intent(this, LoginActivity::class.java))
        else
            startActivity(Intent(this, MailViewActivity::class.java))
        overridePendingTransition(0, 0)
    }
}