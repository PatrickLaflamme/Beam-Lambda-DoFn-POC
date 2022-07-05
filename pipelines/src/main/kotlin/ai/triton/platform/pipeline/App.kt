/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package ai.triton.platform.pipeline

import org.apache.beam.sdk.options.PipelineOptionsFactory


fun main(args: Array<String>) {
    val options = PipelineOptionsFactory.fromArgs(*args).create()
    val pipeline = PocPipeline(options)
    pipeline.run().waitUntilFinish()
}