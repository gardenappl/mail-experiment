package garden.appl.mail.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import garden.appl.mail.mail.MailAccount

class StartActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()

        val account = MailAccount.getCurrent(this)
        if (account == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, MailViewActivity::class.java)
                .putExtra(MailViewActivity.EXTRA_FOLDER_FULL_NAME, "INBOX"))
        }
        overridePendingTransition(0, 0)
    }
}