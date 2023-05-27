package garden.appl.mail

import android.app.Application
import garden.appl.mail.mail.MailAccount

class MailApp : Application() {
    val account get() = MailAccount.getDefault(this)
}