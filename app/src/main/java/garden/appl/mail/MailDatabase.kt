package garden.appl.mail

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import garden.appl.mail.mail.MailFolder
import garden.appl.mail.mail.MailMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Database(
    entities = [MailFolder::class, MailMessage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(MailTypeConverters::class)
abstract class MailDatabase : RoomDatabase() {
    companion object {
        private const val LOGGING_TAG = "DatabaseSetup"

        private val creationMutex = Mutex()

        @Volatile
        private var INSTANCE: MailDatabase? = null

        suspend fun getDatabase(context: Context): MailDatabase =
            INSTANCE ?: creationMutex.withLock {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private suspend fun buildDatabase(context: Context): MailDatabase = withContext(Dispatchers.IO) {
            Room.databaseBuilder(
                context.applicationContext,
                MailDatabase::class.java, "app_database"
            ).run {
//                for (migration in Migrations)
//                    addMigrations(migration)
                build()
            }
        }
    }
}