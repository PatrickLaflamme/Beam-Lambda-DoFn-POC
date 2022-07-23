package ai.triton.platform.dofns


import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.JsonElement
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.windowing.BoundedWindow
import org.apache.beam.sdk.values.TupleTag
import java.io.Serializable as JavaSerializable

abstract class FailureCollectingDoFn<T : ai.triton.platform.dofns.Serializable, R : ai.triton.platform.dofns.Serializable> : DoFn<T, R>() {
    val failuresTag = object: TupleTag<Failure>() {}

    abstract fun processElement(c: ProcessContext, window: BoundedWindow)

    @ProcessElement
    fun processElementCollectingFailures(c: ProcessContext, window: BoundedWindow) {
        try {
            processElement(c, window)
        } catch (e: Throwable) {
            c.output(
                failuresTag,
                Failure(
                    precursorDataJson = c.element().toJsonElement().toString(),
                    failedClass = this::class.qualifiedName ?: this::javaClass.name,
                    exceptionName = e::class.qualifiedName ?: e::javaClass.name,
                    exceptionMessage = e.message,
                    stackTrace = e.stackTrace.map(StackTraceElement::toString)
                ),
            )
        }
    }
}

@Serializable
data class Failure(
    val precursorDataJson: String,
    val failedClass: String,
    val exceptionName: String,
    val exceptionMessage: String?,
    val stackTrace: List<String>,
): JavaSerializable