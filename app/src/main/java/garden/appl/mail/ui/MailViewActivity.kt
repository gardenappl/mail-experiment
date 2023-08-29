package garden.appl.mail.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.ViewModelInitializer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import garden.appl.mail.MailDatabase
import garden.appl.mail.MailTypeConverters
import garden.appl.mail.R
import garden.appl.mail.crypt.AutocryptSetupMessage
import garden.appl.mail.databinding.ActivityMailViewBinding
import garden.appl.mail.mail.MailAccount
import garden.appl.mail.mail.MailFolder
import garden.appl.mail.mail.MailMessageViewModel
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception


private const val LOGGING_TAG = "MailViewActivity"

class MailViewActivity : AppCompatActivity(),
    CoroutineScope by MainScope(), NavigationView.OnNavigationItemSelectedListener
{
    private lateinit var _binding: ActivityMailViewBinding
    private val binding get() = _binding

    private lateinit var messagesViewModel: MailMessageViewModel
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    companion object {
        const val EXTRA_FOLDER_FULL_NAME = "folder"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMailViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actionBarDrawerToggle = ActionBarDrawerToggle(this, binding.root,
            R.string.nav_drawer_open, R.string.nav_drawer_close)
        binding.root.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        launch {
            updateNavigationItems()
        }

        binding.navigation.setNavigationItemSelectedListener(this)

        binding.composeButton.setOnClickListener {
            startActivity(Intent(this, SelectRecipientActivity::class.java))
        }

        val adapter = MailViewAdapter(this)
        binding.messagesList.adapter = adapter
        binding.messagesList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        launch {
            val folder = withContext(Dispatchers.IO) {
                MailDatabase.getDatabase(this@MailViewActivity)
                    .folderDao.getFolder(intent.getStringExtra(EXTRA_FOLDER_FULL_NAME)!!)!!
            }

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
            launch {
                withContext(Dispatchers.IO) {
                    val account = MailAccount.getCurrent(this@MailViewActivity)!!

                    val found = AutocryptSetupMessage.findExisting(account)
                    Log.d(LOGGING_TAG, "Found setup msg? $found")
                    if (found == null) {
                        val (message, passphrase) =
                            AutocryptSetupMessage.generate(account, this@MailViewActivity)
                        val dialogBuilder = AlertDialog.Builder(this@MailViewActivity).apply {
                            setTitle(R.string.autocrypt_setup_dialog_title)
                            setMessage(getString(R.string.autocrypt_setup_dialog, String(passphrase.chars!!)))
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                launch {
                                    Log.d(LOGGING_TAG, "Just gonna send it")
                                    account.send(message,
                                        arrayOf(InternetAddress(account.originalAddress)))
                                }
                            }
                            setNegativeButton(android.R.string.cancel) { _, _ ->
                                // nothing
                            }
                        }
                        passphrase.clear()

                        withContext(Dispatchers.Main) {
                            dialogBuilder.show()
                        }
                    }


                    try {
                        account.connectToStore().use { store ->
                            val currentFolderName = intent.getStringExtra(EXTRA_FOLDER_FULL_NAME)
                            Log.d(LOGGING_TAG, "Loading $currentFolderName")
                            MailTypeConverters.toDatabase(store.getFolder(currentFolderName))
                                .refreshDatabaseMessages(this@MailViewActivity, store)

                            MailTypeConverters.toDatabase(store.defaultFolder)
                                .syncFoldersRecursive(this@MailViewActivity, store)
                            withContext(Dispatchers.Main) {
                                updateNavigationItems()
                            }
                        }
                    } catch (mex: MessagingException) {
                        var ex: Exception? = mex
                        do {
                            ex?.printStackTrace()
                            ex = (ex as? MessagingException)?.nextException
                        } while (ex != null)
                    }
                }
                binding.swipe.isRefreshing = false
            }
        }
    }

//    private fun debugFolder(folder: Folder) {
//        Log.d(LOGGING_TAG, "Name: ${folder.name}")
//        Log.d(LOGGING_TAG, "FUll name: ${folder.fullName}")
//        Log.d(LOGGING_TAG, "URL: ${folder.urlName}")
//
//        if (!folder.isSubscribed) Log.d(LOGGING_TAG, "Not Subscribed")
//        if (folder.type and Folder.HOLDS_MESSAGES != 0) {
//            if (folder.hasNewMessages()) Log.d(LOGGING_TAG, "Has New Messages")
//            Log.d(LOGGING_TAG, "Total Messages:  " + folder.messageCount)
//            Log.d(LOGGING_TAG, "New Messages:    " + folder.newMessageCount)
//            Log.d(LOGGING_TAG, "Unread Messages: " + folder.unreadMessageCount)
//        }
//        if ((folder.type and Folder.HOLDS_FOLDERS) != 0) Log.d(LOGGING_TAG, "Is Directory")
//
//        /*
//         * Demonstrate use of IMAP folder attributes
//         * returned by the IMAP LIST response.
//         */
//        if (folder is IMAPFolder) {
//            val attrs = folder.attributes
//            if (attrs != null && attrs.isNotEmpty()) {
//                Log.d(LOGGING_TAG, "IMAP Attributes:")
//                for (i in attrs.indices) Log.d(LOGGING_TAG, attrs[i])
//            }
//        }
//
//        if ((folder.type and Folder.HOLDS_FOLDERS) != 0) {
//            val f = folder.list()
//            for (i in f.indices) debugFolder(f[i])
//        }
//
//        if (folder.fullName == "INBOX") {
//            folder.open(Folder.READ_WRITE)
//            val messages = folder.messages
//            for (message in messages) {
//                Log.d(LOGGING_TAG, "msg num: ${message.messageNumber}")
//                val mimeMessage = message as MimeMessage
//                val out = ByteArrayOutputStream(1024 * 1024)
//                mimeMessage.writeTo(out)
//                val array = out.toByteArray()
//
//                val newMessage = MimeMessage(mimeMessage.session, ByteArrayInputStream(array))
//                if (newMessage.isMimeType("text/plain")) {
//                    Log.d(LOGGING_TAG, "msg: ${newMessage.content}")
//                } else {
//                    Log.d(LOGGING_TAG, "content type: ${newMessage.contentType}")
//                }
//                for (header in newMessage.allHeaders)
//                    Log.d(LOGGING_TAG, "header ${header.name}: ${header.value}")
//            }
//            folder.close()
//        }
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item))
            return true

        return super.onOptionsItemSelected(item)
    }

    private suspend fun updateNavigationItems() {
        val navigationView = binding.navigation
        navigationView.menu.clear()

        val db = MailDatabase.getDatabase(this@MailViewActivity)
        val folders = db.folderDao.getAllFolders()
        for (folder in folders) {
            navigationView.menu.add(folder.fullName)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.root.closeDrawers()
        startActivity(
            Intent(this, MailViewActivity::class.java)
                .putExtra(EXTRA_FOLDER_FULL_NAME, item.title)
        )
        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}