package ai.triton.platform.dofns


import ai.triton.platform.config.LambdaFunction
import ai.triton.platform.models.AWSConfig
import com.amazonaws.SDKGlobalConfiguration
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.http.apache.client.impl.SdkHttpClient
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.InvokeRequest
import com.amazonaws.services.lambda.model.LogType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.transforms.windowing.BoundedWindow
import org.apache.beam.sdk.values.TupleTagList
import org.apache.beam.vendor.grpc.v1p43p2.io.netty.util.AttributeMap
import org.joda.time.Instant
import java.nio.charset.StandardCharsets
import ai.triton.platform.dofns.Serializable as SerializableType


class FailedLambdaException(messages: List<String>) : Exception(messages.joinToString("\n"))


inline fun <reified T : SerializableType, reified R : SerializableType> FailureAwarePCollection<T>.transformWithLambda(
    clientConfig: AWSConfig,
    batchSize: Int,
    function: LambdaFunction<T, R>,
): FailureAwarePCollection<R> {
    val invokeLambdaTransform = InvokeLambdaTransform(
        clientConfig, batchSize, function
    )
    val parDo = ParDo.of(invokeLambdaTransform).withOutputTags(
            function.validTupleTag, TupleTagList.of(invokeLambdaTransform.failuresTag)
        )
    val resultTuple = data.apply(function.name, parDo)
    val validResults = resultTuple.get(function.validTupleTag)
    val currentFailures = resultTuple.get(invokeLambdaTransform.failuresTag)
    return FailureAwarePCollection(
        validResults, failures + listOf(currentFailures)
    )
}

class InvokeLambdaTransform<T : ai.triton.platform.dofns.Serializable, R : ai.triton.platform.dofns.Serializable>(
    val clientConfig: AWSConfig, val batchSize: Int, val function: LambdaFunction<T, R>
) : FailureCollectingDoFn<T, R>() {

    private val requestCache: MutableList<RequestRecord<T>> = mutableListOf()
    private val windowsById: MutableMap<Int, Pair<BoundedWindow, Instant>> = mutableMapOf()
    private val responseCache: MutableList<ResponseRecord<R>> = mutableListOf()
    private val failureCache: MutableList<Triple<Failure, BoundedWindow, Instant>> = mutableListOf()

    @delegate:Transient
    private val client: AWSLambda by lazy {
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true")
        val builder = AWSLambdaClientBuilder.standard()
        clientConfig.endpoint
            ?.let {
                builder.withEndpointConfiguration(
                    AwsClientBuilder.EndpointConfiguration(
                        clientConfig.endpoint, clientConfig.region
                    ))
            }
            ?: builder.withRegion(clientConfig.region)
        builder.build()
    }

    @StartBundle
    fun startBundle() {
        requestCache.clear()
        responseCache.clear()
        failureCache.clear()
        windowsById.clear()
    }

    override fun processElement(c: ProcessContext, window: BoundedWindow) {
        requestCache.add(RequestRecord(id = c.element().hashCode(), data = c.element()))
        windowsById[c.element().hashCode()] = Pair(window, c.timestamp())
        c.timestamp()
        if (requestCache.size >= batchSize) {
            invoke()
            requestCache.clear()
        }
    }

    @FinishBundle
    fun finishBundle(fbc: FinishBundleContext) {
        invoke()
        responseCache.forEach { response ->
            response.data?.let { data ->
                    windowsById[response.id]?.let { (window, timestamp) ->
                            fbc.output(data, timestamp, window)
                            return@forEach
                        }
                    throw IllegalStateException()
                }
            throw IllegalStateException()
        }
        failureCache.forEach { data ->
            fbc.output(failuresTag, data.first, data.third, data.second)
        }
    }

    private fun invoke() {
        try {
            val requestPayload = RequestPayload(records = requestCache)
            val requestString = Json.encodeToString(requestPayload)
            val request =
                InvokeRequest().withFunctionName(function.name).withPayload(requestString).withLogType(LogType.Tail)
            println("Invoking ${function.name}")
            val response = client.invoke(request)
            val result = String(response.payload.array(), StandardCharsets.UTF_8)
            val responsePayload = Json.decodeFromString<ResponsePayload<R>>(result)
            responsePayload.records.forEach { record ->
                record.data?.let {
                    responseCache.add(record)
                    return@forEach
                }
                record.errorMessages?.let { errorMessages ->
                    val throwable = FailedLambdaException(errorMessages)
                    val failure =
                        Failure(precursorDataJson = requestCache.find { request -> request.id == record.id }
                            ?.data
                            ?.toJsonElement()
                            ?.toString()
                            ?: throw IllegalStateException(),
                            failedClass = this::class.qualifiedName ?: this::javaClass.name,
                            exceptionMessage = throwable.message,
                            exceptionName = throwable::class.qualifiedName ?: throwable::javaClass.name,
                            stackTrace = throwable.stackTrace.map(StackTraceElement::toString))
                    windowsById[record.id]?.let { (window, timestamp) ->
                            failureCache.add(Triple(failure, window, timestamp))
                            return@forEach
                        }
                    throw IllegalStateException()
                }
            }
        } catch (e: Throwable) {
            requestCache.forEach { request ->
                val failure = Failure(
                    precursorDataJson = request.data.toJsonElement().toString(),
                    failedClass = this::class.qualifiedName ?: this::javaClass.name,
                    exceptionMessage = e.message,
                    exceptionName = e::class.qualifiedName ?: e::javaClass.name,
                    stackTrace = e.stackTrace.map(StackTraceElement::toString)
                )
                windowsById[request.id]?.let { (window, timestamp) ->
                        failureCache.add(Triple(failure, window, timestamp))
                        return@forEach
                    }
                throw IllegalStateException()
            }
        }
    }

    @Serializable
    private data class RequestPayload<T>(
        @SerialName("Records") val records: List<RequestRecord<T>>
    )

    @Serializable
    private data class RequestRecord<T>(
        val id: Int,
        val data: T,
    )

    @Serializable
    private data class ResponseRecord<R>(
        val id: Int, val data: R?, val errorMessages: List<String>?
    )

    @Serializable
    private data class ResponsePayload<R>(
        @SerialName("Records") val records: List<ResponseRecord<R>>
    )
}