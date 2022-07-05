package ai.triton.platform.pipeline

import ai.triton.platform.dofns.*
import ai.triton.platform.lambda.ExamplePythonLambda
import ai.triton.platform.model.StringData
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.transforms.Create
import org.apache.beam.sdk.values.PCollectionList

class PocPipeline(options: PipelineOptions) : Pipeline(options) {
    val client: AWSLambda = AWSLambdaClientBuilder.standard().withEndpointConfiguration(
            AwsClientBuilder.EndpointConfiguration(
                "http://local-lambda-local-deploy", "us-east-2"
            )
        ).build()

    init {
        val input = this.apply(
            "load data", Create.of(
                "{\"current\":\"a\"}", "{\"current\":\"b\"}", "{\"current\":\"c\"}"
            )
        )
        val transformsWithFailures =
            input.mapWithFailures("") { string -> Json.decodeFromString<StringData>(string) }
                .transformWithLambda(
                    client = client, batchSize = 100, function = ExamplePythonLambda()
                ).transformWithLambda(
                    client = client, batchSize = 100, function = ExamplePythonLambda(name = "SomeOtherName")
                ).mapWithFailures("") { stringData -> Json.encodeToString(stringData) }

        transformsWithFailures.data.apply("Write failures to disk", TextIO.write().to("~/data/beam-output"))

        transformsWithFailures.failures.forEachIndexed { index, failurePCollection ->
            failurePCollection.map("Serialize Failures") { failure -> Json.encodeToString(failure) }
            .apply("Write failures to disk", TextIO.write().to("~/data/beam-output-failures${index}"))
        }

    }
}