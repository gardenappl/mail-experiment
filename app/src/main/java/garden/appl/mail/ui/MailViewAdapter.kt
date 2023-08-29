package garden.appl.mail.ui

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
        var colorSecondary = com.google.android.material.R.color.m3_default_color_secondary_text
        var colorPrimary = R.color.colorForeground
        var colorBackground = R.color.white
        if (message.contentType.startsWith("multipart/encrypted", ignoreCase = true)) {
            colorSecondary = R.color.white
            colorPrimary = R.color.white
            colorBackground = R.color.teal_700
        }
        binding.subject.setTextColor(context.resources.getColor(colorPrimary))
        for (view in listOf(binding.from, binding.date)) {
            view.setTextColor(context.resources.getColor(colorSecondary))
        }
        binding.root.background = ColorDrawable(context.resources.getColor(colorBackground))

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
