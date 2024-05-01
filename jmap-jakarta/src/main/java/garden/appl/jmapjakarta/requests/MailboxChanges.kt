package garden.appl.jmapjakarta.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Mailbox/changes")
data class MailboxChanges(
    val accountId: String? = null,
    val sinceState: String? = null,
    val maxChanges: Int? = null
) : JMAPRequestMethod()