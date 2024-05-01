package garden.appl.jmapjakarta.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Mailbox/get")
data class MailboxGet(
    val accountId: String? = null,
    val ids: List<String>? = null,
    val properties: List<String>? = null
) : JMAPRequestMethod()