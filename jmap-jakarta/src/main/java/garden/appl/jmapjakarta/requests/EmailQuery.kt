package garden.appl.jmapjakarta.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Email/query")
data class EmailQuery(
    val accountId: String? = null,
    val filter: EmailQuery.FilterCondition? = null,
    val sort: List<Comparator>? = null,
    val position: Int? = null,
    val limit: Int? = null,
    val calculateTotal: Boolean? = null
) : JMAPRequestMethod() {
    @Serializable
    data class FilterCondition(
        val inMailbox: String? = null,
        val from: String? = null,
        val to: String? = null,
        val text: String? = null,
        val subject: String? = null,
        val header: List<String>? = null
    )
}