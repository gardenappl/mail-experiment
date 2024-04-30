package garden.appl.jmapjakarta.impl.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Mailbox/get")
internal data class MailboxGet(
    val accountId: String? = null,
    val ids: List<String>? = null,
    val properties: List<String>? = null
) : JMAPRequestMethod()