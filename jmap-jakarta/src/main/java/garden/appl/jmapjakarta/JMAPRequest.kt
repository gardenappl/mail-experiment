package garden.appl.jmapjakarta

import kotlinx.serialization.Serializable

@Serializable
data class JMAPRequest(
    val using: List<String> = listOf(JMAPConstants.CORE, JMAPConstants.MAIL, JMAPConstants.SUBMISSION),
    val methodCalls: List<JMAPInvocation>
)