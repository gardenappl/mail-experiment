package garden.appl.mail.mail

import androidx.lifecycle.LiveData
import jakarta.mail.Folder

class MailMessageRepository(mailMessageDao: MailMessageDao, folder: MailFolder) {
    val messages: LiveData<List<MailMessage>> = mailMessageDao.getMessages(folder)
}