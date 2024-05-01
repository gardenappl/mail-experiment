package garden.appl.jmapjakarta.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Email/query")
data class EmailQuery(
    val accountId: String? = null,
    val queryState: String? = null,
    val canCalculateChanges: Boolean? = null,
    val position: Int? = null,

) : JMAPResponseMethod()