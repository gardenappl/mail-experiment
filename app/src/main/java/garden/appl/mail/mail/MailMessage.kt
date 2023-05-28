package garden.appl.mail.mail

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = MailMessage.TABLE_NAME)
data class MailMessage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = LOCAL_ID)
    val localId: Int,

    @ColumnInfo(name = FOLDER)
    val folderFullName: String,

    @ColumnInfo(name = MESSAGE_ID)
    val messageID: String?,

    @ColumnInfo(name = FROM)
    val from: String,

    @ColumnInfo(name = TO)
    val to: String,

    @ColumnInfo(name = SUBJECT)
    val subject: String,

    @ColumnInfo(name = CONTENT_TYPE)
    val contentType: String,

    @ColumnInfo(name = DATE)
    val date: Date,

    @ColumnInfo(name = EFFECTIVE_DATE)
    val effectiveDate: Date,

    @ColumnInfo(name = AUTOCRYPT_HEADER)
    val autocryptHeader: String?,

    @ColumnInfo(name = IN_REPLY_TO)
    val inReplyTo: String?,

    @ColumnInfo(name = BLOB, typeAffinity = ColumnInfo.BLOB)
    val blob: ByteArray
) {
    companion object {
        const val TABLE_NAME = "msgs"

        const val LOCAL_ID = "id"
        const val FOLDER = "folder"
        const val MESSAGE_ID = "message_id"
        const val FROM = "from"
        const val TO = "to"
        const val SUBJECT = "subject"
        const val CONTENT_TYPE = "content_type"
        const val DATE = "date"
        const val EFFECTIVE_DATE = "effective_date"
        const val AUTOCRYPT_HEADER = "autocrypt"
        const val IN_REPLY_TO = "in_reply_to"
        const val BLOB = "blob"

        const val BLOB_SIZE = 1024 * 1024
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MailMessage

        if (localId != other.localId) return false
        if (folderFullName != other.folderFullName) return false
        if (messageID != other.messageID) return false
        if (from != other.from) return false
        if (to != other.to) return false
        if (contentType != other.contentType) return false
        if (date != other.date) return false
        if (effectiveDate != other.effectiveDate) return false
        if (autocryptHeader != other.autocryptHeader) return false
        if (inReplyTo != other.inReplyTo) return false
        if (!blob.contentEquals(other.blob)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = localId
        result = 31 * result + (messageID?.hashCode() ?: 0)
        result = 31 * result + folderFullName.hashCode()
        result = 31 * result + from.hashCode()
        result = 31 * result + to.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + effectiveDate.hashCode()
        result = 31 * result + (autocryptHeader?.hashCode() ?: 0)
        result = 31 * result + (inReplyTo?.hashCode() ?: 0)
        result = 31 * result + blob.contentHashCode()
        return result
    }
}