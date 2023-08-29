package garden.appl.mail.mail

import android.content.Context
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
import org.eclipse.angus.mail.iap.CommandFailedException
import org.eclipse.angus.mail.imap.IMAPStore

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

    suspend fun refreshDatabaseMessages(context: Context, store: IMAPStore) {
        val db = MailDatabase.getDatabase(context).messageDao

        withContext(Dispatchers.IO) {
            val folder = store.getFolder(fullName)
            try {
                folder.open(Folder.READ_ONLY)
                for (i in folder.messageCount downTo 1) {
                    val message = folder.getMessage(i) as MimeMessage
                    val existingMessage = db.getMessage(message.messageID)
                    if (existingMessage != null) {
                        Log.d("MailFolder", "Reached existing message at $i: $existingMessage")
                        break
                    }
                    db.insert(message)
                }
            } catch (e: Exception) {
                Log.e("MailFolder", "could not sync", e)
            } finally {
                folder.close()
            }
        }
    }

    suspend fun syncFoldersRecursive(context: Context, store: IMAPStore) {
        val db = MailDatabase.getDatabase(context)
        val folder = store.getFolder(fullName)

        for (subFolder in folder.list()) {
            if (subFolder.fullName == folder.fullName)
                continue

            try {
                val mailSubFolder = MailTypeConverters.toDatabase(subFolder)
                Log.d(MailAccount.LOGGING_TAG, "Got $mailSubFolder")
                db.folderDao.insert(mailSubFolder)
                mailSubFolder.syncFoldersRecursive(context, store)
            } catch (e: CommandFailedException) {
                Log.w(MailAccount.LOGGING_TAG, "could not sync ${folder.fullName}", e)
            }
        }
    }
}