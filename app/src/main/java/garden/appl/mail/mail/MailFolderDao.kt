package garden.appl.mail.mail

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import garden.appl.mail.mail.MailFolder.Companion.FULL_NAME
import garden.appl.mail.mail.MailFolder.Companion.TABLE_NAME

@Dao
abstract class MailFolderDao {
    @Query("SELECT * FROM $TABLE_NAME WHERE $FULL_NAME = :fullName")
    abstract fun getFolder(fullName: String): MailFolder?

    @Insert
    abstract fun insert(folder: MailFolder)
}