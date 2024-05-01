package garden.appl.jmapjakarta

import kotlinx.serialization.Serializable

@Serializable
data class JMAPResponse(
    val methodResponses: List<JMAPInvocation>,
    val sessionState: String? = null
)