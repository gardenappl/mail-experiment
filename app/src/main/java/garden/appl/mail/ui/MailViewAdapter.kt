package garden.appl.mail.ui

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import garden.appl.mail.R
import garden.appl.mail.databinding.ViewInboxMsgBinding
import garden.appl.mail.mail.MailMessage
import jakarta.mail.Session
import kotlinx.coroutines.launch

class MailViewAdapter internal constructor(
    private val activity: Activity
) : RecyclerView.Adapter<MailViewAdapter.MessageViewHolder>() {

    companion object {
        private const val LOGGING_TAG = "MailViewAdapter"
    }

    private val context: Context = activity

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    var messagesList = emptyList<MailMessage>()
        internal set(value) {
            field = value
            notifyDataSetChanged()
        }


    inner class MessageViewHolder(val binding: ViewInboxMsgBinding)
        : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ViewInboxMsgBinding.inflate(inflater, parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messagesList[position]
        val binding = holder.binding

//        binding.from.text = context.getString(R.string.from, message.from)
        binding.from.text = message.from
        binding.date.text = DateUtils.getRelativeDateTimeString(context,
            message.date.time,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
        binding.subject.text = message.subject
        binding.root.setOnClickListener {
            context.startActivity(Intent(context, MessageReadActivity::class.java)
                .putExtra(MessageReadActivity.EXTRA_MESSAGE_LOCAL_ID, message.localId))
        }
    }

    override fun getItemCount(): Int = messagesList.size
}
