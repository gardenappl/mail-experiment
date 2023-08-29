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
import garden.appl.mail.mail.MailMessageViewModel
import jakarta.mail.MessagingException
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.pgpainless.PGPainless
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
        const val EXTRA_FIRST_VISIT = "first"
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
            messagesViewModel.messages.observe(this@MailViewActivity) { messages ->
                messages?.let {
                    adapter.messagesList = messages
                }
            }
            if (intent.getBooleanExtra(EXTRA_FIRST_VISIT, false)) {
                val account = MailAccount.getCurrent(this@MailViewActivity)!!

                launch(Dispatchers.IO) {
                    account.connectToStore().use { store ->
                        val currentFolderName = intent.getStringExtra(EXTRA_FOLDER_FULL_NAME)
                        Log.d(LOGGING_TAG, "Loading $currentFolderName")
                        withContext(Dispatchers.IO) {
                            MailTypeConverters.toDatabase(store.getFolder(currentFolderName))
                                .refreshDatabaseMessages(this@MailViewActivity, store)
                        }
                    }
                }
            }
        }

        binding.swipe.setOnRefreshListener {
            launch {
                withContext(Dispatchers.IO) {
                    val account = MailAccount.getCurrent(this@MailViewActivity)!!

                    Log.d(LOGGING_TAG, "Key: ${PGPainless.asciiArmor(account.keyRing)}")
//                    AutocryptSetupMessage.bootstrapFrom(account, AutocryptSetupMessage.findExisting(account)!!)

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