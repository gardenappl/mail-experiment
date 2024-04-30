package garden.appl.jmapjakarta.impl

import garden.appl.jmapjakarta.impl.requests.JMAPRequestMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = JMAPRequestInvocationSerializer::class)
internal data class JMAPRequestInvocation(
    val method: JMAPRequestMethod,
    val callId: Int
)

internal object JMAPRequestInvocationSerializer : JsonTransformingSerializer<JMAPRequestInvocation>(
    JMAPRequestInvocation.serializer()
) {
    override fun transformSerialize(element: JsonElement): JsonElement {
        val method = element.jsonObject["method"]!!.jsonObject

        return JsonArray(listOf(
            method["%methodName"]!!.jsonPrimitive,
            JsonObject(method.toMutableMap().also { map ->
                map.remove("%methodName")
            }),
            element.jsonObject["callId"]!!.jsonPrimitive
        ))
    }
}