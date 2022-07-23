package ai.triton.platform.config

import org.apache.beam.sdk.values.TupleTag


abstract class LambdaFunction<T,R>(
  val name: String,
  val validTupleTag: TupleTag<R>,
): java.io.Serializable