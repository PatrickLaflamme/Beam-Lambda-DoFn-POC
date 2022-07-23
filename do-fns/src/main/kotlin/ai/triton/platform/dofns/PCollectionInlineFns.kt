package ai.triton.platform.dofns

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.apache.beam.sdk.coders.NullableCoder
import org.apache.beam.sdk.transforms.InferableFunction
import org.apache.beam.sdk.transforms.MapElements
import org.apache.beam.sdk.transforms.SimpleFunction
import org.apache.beam.sdk.transforms.WithFailures.ExceptionElement
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.sdk.values.TypeDescriptor


inline fun <reified I, reified T> PCollection<I>.mapWithFailures(
    name: String? = null,
    noinline closure: (I) -> T,
): FailureAwarePCollection<T> {
    val resolvedName = name ?: "map to ${T::class.simpleName}"
    val elementHandler = object: InferableFunction<I, T>() {
        override fun apply(input: I): T {
            return closure(input)
        }
    }
    val failureHandler = object: ExceptionAsFailure<I>(resolvedName) {}
    val mapWithFailures = this.apply(
        resolvedName,
        MapElements.into(TypeDescriptor.of(T::class.java))
            .via(elementHandler)
            .exceptionsVia(failureHandler)
    )

    val pc = mapWithFailures.output()

    return FailureAwarePCollection(
        pc,
        mutableListOf(mapWithFailures.failures())
    )
}

inline fun <reified I, reified T> PCollection<I>.map(
    name: String? = null,
    noinline closure: (I) -> T,
): PCollection<T> {
    val resolvedName = name ?: "map to ${T::class.simpleName}"
    val elementHandler = object: InferableFunction<I, T>() {
        override fun apply(input: I): T {
            return closure(input)
        }
    }
    val pc = this.apply(
        resolvedName,
        MapElements.into(TypeDescriptor.of(T::class.java))
            .via(elementHandler)
    )

    return pc
}

class FailureAwarePCollection<T>(
    val data: PCollection<T>,
    val failures: List<PCollection<Failure>>
)

inline fun <reified I, reified T> FailureAwarePCollection<I>.mapWithFailures(
    name: String? = null,
    noinline closure: (I) -> T,
): FailureAwarePCollection<T> {
    val resolvedName = name ?: "map to ${T::class.simpleName}"
    val elementHandler = object: InferableFunction<I, T>() {
        override fun apply(input: I): T {
            return closure(input)
        }
    }
    val failureHandler = object: ExceptionAsFailure<I>(resolvedName) {}
    val mapWithFailures = data.apply(
        resolvedName,
        MapElements.into(TypeDescriptor.of(T::class.java))
            .via(elementHandler)
            .exceptionsVia(failureHandler)
    )

    val pc = mapWithFailures.output()
    pc.coder = NullableCoder.of(pc.coder)

    return FailureAwarePCollection(
        pc,
        failures + listOf(mapWithFailures.failures())
    )
}

open class ExceptionAsFailure<T>(private val failedClassName: String) :
    SimpleFunction<ExceptionElement<T>, Failure>() {
    override fun apply(f: ExceptionElement<T>): Failure {
        val exception = f.exception()
        val precursorDataJson: String = when(val element = f.element() as Any) {
            is String -> element
            is Serializable -> element.toJsonElement().toString()
            else -> f.element().toString()
        }
        return Failure(
            precursorDataJson = precursorDataJson,
            failedClass = failedClassName,
            exceptionMessage = exception.message,
            exceptionName = exception::class.qualifiedName ?: exception::javaClass.name,
            stackTrace = exception.stackTrace.map(StackTraceElement::toString)
        )
    }
}