package ai.triton.platform.dofns

import kotlinx.serialization.json.JsonElement
import org.apache.beam.sdk.values.TupleTag

interface Serializable {
    fun toJsonElement(): JsonElement
}