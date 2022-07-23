package ai.triton.platform.model

import kotlinx.serialization.Serializable
import ai.triton.platform.dofns.Serializable as SerializableType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.io.Serializable as JavaSerializable


@Serializable
class StringData(
    val current: String,
    val previous: List<StringData>? = null,
) : SerializableType, JavaSerializable {
    override fun toJsonElement(): JsonElement {
        return Json.encodeToJsonElement(this)
    }
}
