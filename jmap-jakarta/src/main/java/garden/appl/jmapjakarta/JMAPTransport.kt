package garden.appl.jmapjakarta

import jakarta.mail.Address
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.URLName
import jakarta.mail.internet.MimeMessage

class JMAPTransport(session: Session, urlName: URLName) : Transport(session, urlName) {
    override fun sendMessage(msg: Message, addresses: Array<out Address>) {
        if (!(msg is MimeMessage))
            throw IllegalArgumentException("msg must be a MimeMessage")
        TODO("Not yet implemented")
    }
}