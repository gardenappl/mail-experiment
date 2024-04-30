package garden.appl.mail

import android.app.Application
import garden.appl.mail.mail.MailAccount
import kotlinx.serialization.Serializable

@Serializable
data class Project(val inte: Int)
class MailApp : Application() {
    val account get() = MailAccount.getCurrent(this)
}