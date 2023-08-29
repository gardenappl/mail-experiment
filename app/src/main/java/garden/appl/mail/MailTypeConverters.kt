package garden.appl.mail

import androidx.room.TypeConverter
import garden.appl.mail.mail.MailFolder
import garden.appl.mail.mail.MailMessage
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date

class MailTypeConverters {
    companion object {
        private val messageByteArrayOutputStream = ByteArrayOutputStream(MailMessage.BLOB_SIZE)

        @TypeConverter
        @JvmStatic
        fun toDatabase(message: MimeMessage): MailMessage {
            return MailMessage(
                localId = 0,
                messageID = message.messageID,
                folderFullName = message.folder.fullName,
                from = InternetAddress.toString(message.from),
                to = InternetAddress.toString(message.getRecipients(Message.RecipientType.TO)),
                subject = message.subject,
                contentType = message.contentType,
                date = message.sentDate,
                effectiveDate = Date().let { now ->
                    if (message.sentDate.after(now)) now else message.sentDate
                }, //TODO: assert: one valid header
                autocryptHeader = message.getHeader("Autocrypt")?.first(),
                inReplyTo = message.getHeader("In-Reply-To")?.first(),
                blob = messageByteArrayOutputStream.also { stream ->
                    stream.reset()
                    message.writeTo(stream)
                }.toByteArray()
            )
        }

        fun fromDatabase(message: MailMessage, session: Session): MimeMessage {
            val byteStream = ByteArrayInputStream(message.blob)
            return MimeMessage(session, byteStream)
        }

        @TypeConverter
        @JvmStatic
        fun toDatabase(folder: Folder): MailFolder {
            return MailFolder(
                fullName = folder.fullName,
                name = folder.name,
                url = folder.urlName.toString(),
                totalMessages = if (folder.type and Folder.HOLDS_MESSAGES != 0)
                    folder.messageCount
                else -1,
                unreadMessages = if (folder.type and Folder.HOLDS_MESSAGES != 0)
                    folder.unreadMessageCount
                else -1,
                isDirectory = folder.type and Folder.HOLDS_FOLDERS != 0
            )
        }

        @TypeConverter
        @JvmStatic
        fun toDatabase(date: Date): Long = date.time

        @TypeConverter
        @JvmStatic
        fun toDate(unixTime: Long): Date = Date(unixTime)
    }
}