package garden.appl.mail

import android.app.Application
import garden.appl.mail.mail.MailAccount

class MailApp : Application() {
    companion object {
        lateinit var account: MailAccount
    }
}