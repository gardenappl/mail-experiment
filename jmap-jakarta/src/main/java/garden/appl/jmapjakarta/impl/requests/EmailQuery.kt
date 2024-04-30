package garden.appl.jmapjakarta.impl.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Email/query")
internal data class EmailQuery(
    val accountId: String? = null,
    val filter: EmailQueryFilterCondition? = null,
    val sort: List<Comparator>? = null,
    val position: Int? = null,
    val limit: Int? = null,
    val calculateTotal: Boolean? = null
) : JMAPRequestMethod()

@Serializable
internal data class EmailQueryFilterCondition(
    val inMailbox: String? = null,
    val from: String? = null,
    val to: String? = null,
    val text: String? = null,
    val subject: String? = null,
    val header: List<String>? = null
)