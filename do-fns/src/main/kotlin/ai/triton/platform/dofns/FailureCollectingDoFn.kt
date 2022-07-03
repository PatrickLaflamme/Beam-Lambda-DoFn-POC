package ai.triton.platform.dofns


import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.LogType;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;
import kotlinx.serialization.Serializable;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.encodeToString;
import kotlinx.serialization.decodeFromString;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.DoFn.ProcessContext;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.values.TupleTag
import org.joda.time.Instant;


import ai.triton.platform.config.LambdaFunction;
import java.nio.charset.StandardCharsets;


abstract class FailureCollectingDoFn<T,R>(): DoFn<T, R>() {
  val className = this::class.qualifiedName;
  val validTag = TupleTag<R>();
  val failuresTag = TupleTag<Failure<T,R>>();

  abstract fun processElement(c: ProcessContext, window: BoundedWindow);

  @ProcessElement
  fun processElementCollectingFailures(c: ProcessContext, window: BoundedWindow) {
    try {
      processElement(c, window);
    } catch (e: Throwable) {
      c.output(failuresTag, Failure(c.element(), this.className, e));
    }
  }
}

class Failure<T,R> {

    val failedClass: String?;
    val message: String?;
    val precursorDataJson: String;
    val stackTrace: List<String>;

    public constructor(
        precursorData: T,
        transformClass: String,
        thrown: Throwable,
    ) {
        failedClass = transformClass;
        message = thrown.message;
        precursorDataJson = Json.encodeToString(precursorData);
        stackTrace = thrown.stackTrace.map({ stackTraceElement -> stackTraceElement.toString() });
    }

}