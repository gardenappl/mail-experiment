package garden.appl.mail.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.ViewModelInitializer
import androidx.recyclerview.widget.LinearLayoutManager
import garden.appl.mail.MailDatabase
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.databinding.ActivityMailViewBinding
import garden.appl.mail.mail.MailAccount
import garden.appl.mail.mail.MailMessageViewModel
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.eclipse.angus.mail.imap.IMAPFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception


private const val LOGGING_TAG = "MailViewActivity"

class MailViewActivity : AppCompatActivity(), CoroutineScope by MainScope()  {
    private lateinit var _binding: ActivityMailViewBinding
    private val binding get() = _binding

    private lateinit var messagesViewModel: MailMessageViewModel

    companion object {
        const val EXTRA_FOLDER_FULL_NAME = "folder"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMailViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.composeButton.setOnClickListener {
        }

        val adapter = MailViewAdapter(this)
        binding.messagesList.adapter = adapter
        binding.messagesList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        launch(Dispatchers.IO) {
            val folder = MailDatabase.getDatabase(this@MailViewActivity)
                .folderDao.getFolder(intent.getStringExtra(EXTRA_FOLDER_FULL_NAME)!!)!!

            this@MailViewActivity.supportActionBar?.title = folder.name

            messagesViewModel = ViewModelProvider(this@MailViewActivity, ViewModelProvider.Factory.from(
                ViewModelInitializer(MailMessageViewModel::class.java) {
                    return@ViewModelInitializer MailMessageViewModel(this@MailViewActivity.application, folder)
                }
            ))[MailMessageViewModel::class.java]
            launch(Dispatchers.Main) {
                messagesViewModel.messages.observe(this@MailViewActivity) { messages ->
                    messages?.let {
                        adapter.messagesList = messages
                    }
                }
            }
        }

        binding.swipe.setOnRefreshListener {
            launch(Dispatchers.IO) {
                val account = MailAccount.getCurrent(this@MailViewActivity)!!

                try {
                    account.connectToStore().use { store ->
                        MailTypeConverters.toDatabase(store.getFolder("INBOX"))
                            .refreshDatabaseMessages(this@MailViewActivity, account)
                    }
                } catch (mex: MessagingException) {
                    var ex: Exception? = mex
                    do {
                        ex?.printStackTrace()
                        ex = (ex as? MessagingException)?.nextException
                    } while (ex != null)
                }
                binding.swipe.isRefreshing = false
            }
        }
    }

    private fun debugFolder(folder: Folder) {
        Log.d(LOGGING_TAG, "Name: ${folder.name}")
        Log.d(LOGGING_TAG, "FUll name: ${folder.fullName}")
        Log.d(LOGGING_TAG, "URL: ${folder.urlName}")

        if (!folder.isSubscribed) Log.d(LOGGING_TAG, "Not Subscribed")
        if (folder.type and Folder.HOLDS_MESSAGES != 0) {
            if (folder.hasNewMessages()) Log.d(LOGGING_TAG, "Has New Messages")
            Log.d(LOGGING_TAG, "Total Messages:  " + folder.messageCount)
            Log.d(LOGGING_TAG, "New Messages:    " + folder.newMessageCount)
            Log.d(LOGGING_TAG, "Unread Messages: " + folder.unreadMessageCount)
        }
        if ((folder.type and Folder.HOLDS_FOLDERS) != 0) Log.d(LOGGING_TAG, "Is Directory")

        /*
         * Demonstrate use of IMAP folder attributes
         * returned by the IMAP LIST response.
         */
        if (folder is IMAPFolder) {
            val attrs = folder.attributes
            if (attrs != null && attrs.isNotEmpty()) {
                Log.d(LOGGING_TAG, "IMAP Attributes:")
                for (i in attrs.indices) Log.d(LOGGING_TAG, attrs[i])
            }
        }

        if ((folder.type and Folder.HOLDS_FOLDERS) != 0) {
            val f = folder.list()
            for (i in f.indices) debugFolder(f[i])
        }

        if (folder.fullName == "INBOX") {
            folder.open(Folder.READ_WRITE)
            val messages = folder.messages
            for (message in messages) {
                Log.d(LOGGING_TAG, "msg num: ${message.messageNumber}")
                val mimeMessage = message as MimeMessage
                val out = ByteArrayOutputStream(1024 * 1024)
                mimeMessage.writeTo(out)
                val array = out.toByteArray()

                val newMessage = MimeMessage(mimeMessage.session, ByteArrayInputStream(array))
                if (newMessage.isMimeType("text/plain")) {
                    Log.d(LOGGING_TAG, "msg: ${newMessage.content}")
                } else {
                    Log.d(LOGGING_TAG, "content type: ${newMessage.contentType}")
                }
                for (header in newMessage.allHeaders)
                    Log.d(LOGGING_TAG, "header ${header.name}: ${header.value}")
            }
            folder.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}