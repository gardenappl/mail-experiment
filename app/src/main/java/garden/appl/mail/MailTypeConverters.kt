package garden.appl.mail

import androidx.room.TypeConverter
import garden.appl.mail.mail.MailFolder
import garden.appl.mail.mail.MailMessage
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.io.ByteArrayOutputStream
import java.util.Date

class MailTypeConverters {
    companion object {
        @TypeConverter
        @JvmStatic
        fun toDatabase(message: MimeMessage): MailMessage {
            return MailMessage(
                localId = 0,
                messageID = message.messageID,
                from = InternetAddress.toString(message.from),
                to = InternetAddress.toString(message.getRecipients(Message.RecipientType.TO)),
                subject = message.subject,
                contentType = message.contentType,
                date = message.sentDate,
                effectiveDate = Date().let { now ->
                    if (message.sentDate.after(now)) now else message.sentDate
                }, //TODO: assert: one valid header
                autocryptHeader = message.getHeader("Autocrypt").first(),
                inReplyTo = message.getHeader("In-Reply-To").first(),
                blob = ByteArrayOutputStream(MailMessage.BLOB_SIZE).also { stream ->
                    message.writeTo(stream)
                }.toByteArray()
            )
        }

        @TypeConverter
        @JvmStatic
        fun toDatabase(folder: Folder): MailFolder {
            return MailFolder(
                fullName = folder.fullName,
                name = folder.name,
                url = folder.urlName.toString(),
                totalMessages = folder.messageCount,
                unreadMessages = folder.unreadMessageCount,
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