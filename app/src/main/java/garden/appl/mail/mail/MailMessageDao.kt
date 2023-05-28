package garden.appl.mail.mail

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.mail.MailMessage.Companion.DATE
import garden.appl.mail.mail.MailMessage.Companion.FOLDER
import garden.appl.mail.mail.MailMessage.Companion.LOCAL_ID
import garden.appl.mail.mail.MailMessage.Companion.MESSAGE_ID
import jakarta.mail.Folder
import garden.appl.mail.mail.MailMessage.Companion.TABLE_NAME
import jakarta.mail.internet.MimeMessage

@Dao
abstract class MailMessageDao {
    fun getMessages(folder: MailFolder): LiveData<List<MailMessage>> {
        return getMessagesForFolderName(folder.fullName)
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE $FOLDER = :fullFolderName ORDER BY $DATE DESC")
    abstract fun getMessagesForFolderName(fullFolderName: String): LiveData<List<MailMessage>>

    @Query("SELECT * FROM $TABLE_NAME WHERE $LOCAL_ID = :localId")
    abstract suspend fun getMessage(localId: Int): MailMessage?

    @Query("DELETE FROM $TABLE_NAME WHERE $MESSAGE_ID = :messageID")
    abstract suspend fun deleteMessageID(messageID: String)

    @Insert
    abstract suspend fun insertRaw(message: MailMessage)

    @Transaction
    open suspend fun insert(message: MimeMessage) {
        message.messageID?.let { deleteMessageID(message.messageID) }
        insertRaw(MailTypeConverters.toDatabase(message))
    }
}