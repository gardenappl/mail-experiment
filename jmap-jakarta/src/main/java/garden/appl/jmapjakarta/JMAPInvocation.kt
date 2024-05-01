package garden.appl.jmapjakarta

import garden.appl.jmapjakarta.requests.JMAPRequestMethod
import garden.appl.jmapjakarta.requests.ResultReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = JMAPInvocationSerializer::class)
data class JMAPInvocation(
    val method: JMAPRequestMethod,
    val resultReferences: Map<String, ResultReference> = emptyMap(),
    val callId: Int
)

private object JMAPInvocationSerializer : JsonTransformingSerializer<JMAPInvocation>(
    JMAPInvocation.serializer()
) {
    override fun transformSerialize(element: JsonElement): JsonElement {
        val method = element.jsonObject["method"]!!.jsonObject
        val resultReferences = element.jsonObject["resultReferences"]!!.jsonObject

        return JsonArray(listOf(
            method["%methodName"]!!.jsonPrimitive,
            JsonObject(method.toMutableMap().also { map ->
                map.remove("%methodName")
                for (entry in resultReferences) {
                    map["#${entry.key}"] = entry.value
                }
            }),
            element.jsonObject["callId"]!!.jsonPrimitive
        ))
    }
}