package garden.appl.jmapjakarta.responses

import garden.appl.jmapjakarta.objects.Email
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Email/get")
data class EmailGet(
    val accountId: String? = null,
    val state: String? = null,
    val list: List<Email>? = null,
    val notFound: List<String>? = null
) : JMAPResponseMethod()