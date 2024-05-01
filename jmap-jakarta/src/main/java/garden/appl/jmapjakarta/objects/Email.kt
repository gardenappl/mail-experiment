package garden.appl.jmapjakarta.objects

import kotlinx.serialization.Serializable


@Serializable
data class Email(
    val id: String? = null,
    val blobId: String? = null,
    val messageId: String? = null,
    val sender: List<EmailAddress>? = null,
    val from: List<EmailAddress>? = null,
    val to: List<EmailAddress>? = null,
    val threadId: String? = null,
    val mailboxIds: List<String>? = null,
    val bodyStructure: EmailBodyPart? = null,
    val bodyValues: Map<String, EmailBodyPart>? = null,
)

@Serializable
data class EmailAddress(
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class EmailHeader(
    val name: String? = null,
    val value: String? = null
)

@Serializable
data class EmailBodyPart(
    val partId: String? = null,
    val blobId: String? = null,
    val headers: List<EmailHeader>? = null,
    val name: String? = null,
    val type: String? = null,
    val subParts: List<EmailBodyPart>? = null
)