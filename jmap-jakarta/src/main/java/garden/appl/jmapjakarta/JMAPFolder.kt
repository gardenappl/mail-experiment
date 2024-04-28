package garden.appl.jmapjakarta

import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store

class JMAPFolder(store: Store?) : Folder(store) {
    override fun close(expunge: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getFullName(): String {
        TODO("Not yet implemented")
    }

    override fun getParent(): Folder {
        TODO("Not yet implemented")
    }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override fun list(pattern: String?): Array<Folder> {
        TODO("Not yet implemented")
    }

    override fun getSeparator(): Char {
        TODO("Not yet implemented")
    }

    override fun getType(): Int {
        TODO("Not yet implemented")
    }

    override fun create(type: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasNewMessages(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getFolder(name: String?): Folder {
        TODO("Not yet implemented")
    }

    override fun delete(recurse: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun renameTo(f: Folder?): Boolean {
        TODO("Not yet implemented")
    }

    override fun open(mode: Int) {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPermanentFlags(): Flags {
        TODO("Not yet implemented")
    }

    override fun getMessageCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getMessage(msgnum: Int): Message {
        TODO("Not yet implemented")
    }

    override fun appendMessages(msgs: Array<out Message>?) {
        TODO("Not yet implemented")
    }

    override fun expunge(): Array<Message> {
        TODO("Not yet implemented")
    }
}