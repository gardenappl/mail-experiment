package garden.appl.mail.mail

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import garden.appl.mail.mail.MailFolder.Companion.FULL_NAME
import garden.appl.mail.mail.MailFolder.Companion.TABLE_NAME

@Dao
abstract class MailFolderDao {
    @Query("SELECT * FROM $TABLE_NAME WHERE $FULL_NAME = :fullName")
    abstract fun getFolder(fullName: String): MailFolder?

    @Query("DELETE FROM $TABLE_NAME WHERE $FULL_NAME = :fullName")
    abstract fun delete(fullName: String)

    @Upsert
    abstract suspend fun insert(folder: MailFolder)

    @Query("SELECT * FROM $TABLE_NAME")
    abstract suspend fun getAllFolders(): List<MailFolder>
}