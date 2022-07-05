package ai.triton.platform.dofns

import ai.triton.platform.plus
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.TupleTag
import org.apache.beam.sdk.values.TupleTagList


class SerializationTransform<T: Serializable>: DoFn<T, String>() {
    val validTag = object: TupleTag<String>() {}
    val failuresTag = object: TupleTag<Failure>() {}

    fun apply(name: String, input: PCollection<T>, existingFailures: PCollection<Failure>? = null): Pair<PCollection<String>, PCollection<Failure>> {
        val parDo = ParDo.of(this)
            .withOutputTags(
                validTag,
                TupleTagList.of(failuresTag)
            )
        val resultTuple = input.apply(name, parDo)
        val validResults = resultTuple.get(validTag)
        val currentFailures = resultTuple.get(failuresTag)
        existingFailures?.let { failures ->
            val concatFailures = failures + currentFailures
            return Pair(validResults, concatFailures)
        }
        return Pair(validResults, currentFailures)
    }

    @ProcessElement
    fun processElement(c: ProcessContext) {
        try {
            val string = Json.encodeToString(c.element().toJsonElement())
            c.output(string)
        } catch (e: Throwable) {
            c.output(
                failuresTag,
                Failure(
                    precursorData = c.element().toJsonElement(),
                    failedClass = this::class.qualifiedName ?: this::javaClass.name,
                    exceptionName = e::class.qualifiedName ?: e::javaClass.name,
                    exceptionMessage = e.message,
                    stackTrace = e.stackTrace.map(StackTraceElement::toString)
                ),
            )
        }
    }
}