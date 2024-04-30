package garden.appl.jmapjakarta.impl.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Email/get")
internal data class EmailGet(
    val accountId: String? = null,
    val ids: List<String>? = null,
    val properties: List<String>? = null,
    val bodyProperties: List<String>? = null,
    val fetchTextBodyValues: Boolean? = null,
    val fetchHTMLBodyValues: Boolean? = null,
    val fetchAllBodyValues: Boolean? = null,
) : JMAPRequestMethod()