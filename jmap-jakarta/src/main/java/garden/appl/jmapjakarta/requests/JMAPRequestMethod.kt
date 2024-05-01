package garden.appl.jmapjakarta.requests

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("%methodName")
sealed class JMAPRequestMethod

@Serializable
data class ResultReference(
    val resultOf: String,
    val name: String,
    val path: String
)

@Serializable
data class Comparator(
    val property: String,
    val isAscending: Boolean
)