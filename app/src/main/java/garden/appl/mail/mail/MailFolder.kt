package garden.appl.mail.mail

import android.content.Context
import android.icu.text.CaseMap.Fold
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import garden.appl.mail.MailDatabase
import garden.appl.mail.MailTypeConverters
import jakarta.mail.Folder
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.SortTerm

@Entity(tableName = MailFolder.TABLE_NAME)
data class MailFolder(
    @PrimaryKey
    @ColumnInfo(name = FULL_NAME)
    val fullName: String,

    @ColumnInfo(name = NAME)
    val name: String,

    @ColumnInfo(name = URL)
    val url: String,

    @ColumnInfo(name = TOTAL_MESSAGES)
    val totalMessages: Int,

    @ColumnInfo(name = UNREAD_MESSAGES)
    val unreadMessages: Int,

    @ColumnInfo(name = IS_DIRECTORY)
    val isDirectory: Boolean
) {
    companion object {
        const val TABLE_NAME = "folders"

        const val FULL_NAME = "full_name"
        const val NAME = "name"
        const val URL = "url"
        const val TOTAL_MESSAGES = "total_msgs"
        const val UNREAD_MESSAGES = "unread_msgs"
        const val IS_DIRECTORY = "is_directory"
    }

    suspend fun refreshDatabaseMessages(context: Context, account: MailAccount) {
        val db = MailDatabase.getDatabase(context).messageDao

        account.connectToStore().use { store ->
            withContext(Dispatchers.IO) {
                val folder = store.getFolder(fullName)
                try {
                    folder.open(Folder.READ_ONLY)
                    for (i in 0 until folder.messageCount) {
                        val message = folder.getMessage(folder.messageCount - 1) as MimeMessage
                        if (db.getMessage(message.messageID) != null)
                            break
                        db.insert(message)
                    }
                } catch (e: Exception) {
                    Log.e("MailFolder", "could not sync", e)
                } finally {
                    folder.close()
                }
            }
        }
    }
}