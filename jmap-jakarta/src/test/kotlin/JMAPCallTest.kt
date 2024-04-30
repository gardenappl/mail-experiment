package garden.appl.jmapjakarta

import garden.appl.jmapjakarta.impl.JMAPRequestInvocation
import garden.appl.jmapjakarta.impl.requests.JMAPRequestMethod
import garden.appl.jmapjakarta.impl.requests.MailboxGet
import junit.framework.TestCase
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JMAPCallTest : TestCase() {
    fun testEncode() {
        val mailboxGet = MailboxGet("afas", listOf("asfa"))
        val invocation = JMAPRequestInvocation(mailboxGet, 0)
        println(Json.encodeToString(invocation))
    }
}