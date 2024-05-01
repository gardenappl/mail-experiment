package garden.appl.mail.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import garden.appl.mail.R

private const val LOGGING_TAG = "LoginActivity"

class LoginActivity : AppCompatActivity() {
    lateinit var viewPager: ViewPager2

    lateinit var address: String
    lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        viewPager = findViewById(R.id.pager)
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        Log.d(LOGGING_TAG, "Hello")
    }

    override fun onBackPressed() {
        if (!this::viewPager.isInitialized || viewPager.currentItem == 0)
            super.onBackPressed()
        else
            viewPager.currentItem--
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        private val fragments = arrayOf(LoginFragment::class.java, ImapSmtpConfigFragment::class.java)

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment {
            return fragments[position].newInstance()
        }
    }
}