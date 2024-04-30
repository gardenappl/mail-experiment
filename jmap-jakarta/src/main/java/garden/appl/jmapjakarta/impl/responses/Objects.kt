package garden.appl.jmapjakarta.impl.responses

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("%methodName")
internal sealed class JMAPResponseMethod

@Serializable
data class Comparator(
    val property: String,
    val isAscending: Boolean
)