package garden.gardenapple.mail

import android.app.Application
import garden.gardenapple.mail.mail.MailAccount

class MailApp : Application() {
    companion object {
        lateinit var account: MailAccount
    }
}