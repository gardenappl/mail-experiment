package garden.appl.jmapjakarta

import jakarta.mail.Folder
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.URLName
import org.eclipse.angus.mail.imap.IMAPStore

class JMAPStore(session: Session?, urlName: URLName) : Store(session, urlName) {
    override fun getDefaultFolder(): Folder {
        TODO("Not yet implemented")
    }

    override fun getFolder(name: String?): Folder {
        TODO("Not yet implemented")
    }

    override fun getFolder(url: URLName?): Folder {
        TODO("Not yet implemented")
    }
}