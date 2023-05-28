package garden.appl.mail.mail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import garden.appl.mail.MailDatabase
import jakarta.mail.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MailMessageViewModel(app: Application, folder: MailFolder) : AndroidViewModel(app) {
    private val repository: MailMessageRepository = runBlocking(Dispatchers.IO) {
        MailMessageRepository(MailDatabase.getDatabase(app).messageDao, folder)
    }

    val messages = repository.messages
}