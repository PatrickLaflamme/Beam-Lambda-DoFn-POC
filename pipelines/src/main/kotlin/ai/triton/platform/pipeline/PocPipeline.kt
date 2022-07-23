package ai.triton.platform.pipeline

import ai.triton.platform.dofns.map
import ai.triton.platform.dofns.mapWithFailures
import ai.triton.platform.dofns.transformWithLambda
import ai.triton.platform.lambda.ExamplePythonLambda
import ai.triton.platform.model.StringData
import ai.triton.platform.models.AWSConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.beam.runners.spark.io.ConsoleIO
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.transforms.Create

class PocPipeline(options: PipelineOptions) : Pipeline(options) {
    val clientConfig: AWSConfig = AWSConfig(
        region = "us-east-2",
        endpoint = "http://localhost:8081"
    )

    init {
        val input = this.apply(
            "load data", Create.of(
                "{\"current\":\"a\"}", "{\"current\":\"b\"}", "{\"current\":\"c\"}"
            )
        )
        input.apply(ConsoleIO.Write.out())
        val transformsWithFailures =
            input.mapWithFailures("") {
                    string -> Json.decodeFromString<StringData>(string) }
                .transformWithLambda(
                    clientConfig = clientConfig, batchSize = 100, function = ExamplePythonLambda(name = "function")
                ).transformWithLambda(
                    clientConfig = clientConfig, batchSize = 100, function = ExamplePythonLambda(name = "function")
                ).mapWithFailures("") { stringData -> Json.encodeToString(stringData) }

        transformsWithFailures.data.apply("Write data to disk", TextIO.write().to("data/beam-output"))

        transformsWithFailures.failures.forEachIndexed { index, failurePCollection ->
            failurePCollection.map("Serialize Failures") { failure -> Json.encodeToString(failure) }
            .apply("Write failures to disk", TextIO.write().to("data/beam-output-failures${index}"))
        }

    }
}