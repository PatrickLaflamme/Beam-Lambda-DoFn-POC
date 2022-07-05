package ai.triton.platform.lambda

import ai.triton.platform.config.LambdaFunction
import ai.triton.platform.model.StringData
import org.apache.beam.sdk.values.TupleTag

class ExamplePythonLambda(
    name: String = "ExamplePythonLambda",
    stack: String = "dev"
): LambdaFunction<StringData, StringData>("$stack-$name", object: TupleTag<StringData>() {})