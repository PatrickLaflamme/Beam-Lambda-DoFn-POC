package ai.triton.platform.model

import ai.triton.platform.dofns.Serializable as SerializableType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.io.Serializable

class StringData(
    val current: String,
    val previous: List<StringData>? = null,
) : SerializableType, Serializable {
    override fun toJsonElement(): JsonElement {
        return Json.encodeToJsonElement(this)
    }
}
