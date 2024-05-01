package garden.appl.jmapjakarta.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Mailbox/changes")
data class MailboxChanges(
    val accountId: String? = null,
    val oldState: String? = null,
    val newState: String? = null,
    val hasNewChanges: Boolean? = null,
    val created: List<String> = emptyList(),
    val updated: List<String> = emptyList(),
    val destroyed: List<String> = emptyList()
) : JMAPResponseMethod()