package garden.appl.mail.mail

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
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
        const val FULL_NAME = "full_name"
        const val NAME = "name"
        const val URL = "url"
        const val TOTAL_MESSAGES = "total_msgs"
        const val UNREAD_MESSAGES = "unread_msgs"
        const val IS_DIRECTORY = "is_directory"
    }
}