package garden.appl.jmapjakarta.objects

import kotlinx.serialization.Serializable

@Serializable
data class Mailbox(
    val id: String? = null,
    val name: String? = null,
    val parentId: String? = null,
    val role: String? = null,
    val totalEmails: Int? = null,
    val unreadEmails: Int? = null
)