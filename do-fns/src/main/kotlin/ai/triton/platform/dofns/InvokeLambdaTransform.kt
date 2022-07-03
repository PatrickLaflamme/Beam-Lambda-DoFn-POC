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
import org.joda.time.Instant;


import ai.triton.platform.config.LambdaFunction;
import java.nio.charset.StandardCharsets;


class InvokeLambdaTransform<T,R>(
  val region: String,
  val batchSize: Int,
  val function: LambdaFunction<T,R>
): FailureCollectingDoFn<T, R>() {

  override val className = this::class.qualifiedName;
  private val requestCache: MutableList<RequestRecord> = mutableListOf();
  private val windowsById: MutableMap<T, BoundedWindow> = mutableMapOf();
  private val responseCache: MutableList<ResponseRecord> = mutableListOf();
  private val client: AWSLambda = AWSLambdaClientBuilder.standard().build();

  override fun processElement(c: ProcessContext, window: BoundedWindow) {
    requestCache.add(c.element());
    windowsById.put(c.element().hashCode(), window);
    if (requestCache.size >= batchSize) {
      invoke();
      requestCache.clear();
    }
  }

  @FinishBundle
  fun finishBundle(fbc: FinishBundleContext) {
    invoke();
    responseCache.forEach{ response -> 
      fbc.output(response);
    };
    requestCache.clear();
    responseCache.clear();
    windowsById.clear();
  }

  private fun invoke() {
    val records = pending.mapNotNull({ elementId -> 
      requestCache.get(elementId)
      ?.let { content ->
        RequestRecord(
          id=content.element.hashCode(), 
          data=content.element, 
        );
      };
    });
    val requestPayload = RequestPayload(Records=records);
    val requestString = Json.encodeToString(requestPayload)
    val request = InvokeRequest()
      .withFunctionName(function.name)
      .withPayload(requestString)
      .withLogType(LogType.Tail);
    val response = client.invoke(request);
    val result = String(response.getPayload().array(), StandardCharsets.UTF_8);
    val responsePayload = Json.decodeFromString<ResponsePayload>(result);
    responsePayload.Records.forEach { record ->
      responseCache.add(record);
    }
  }

  @Serializable
  private inner class RequestPayload(
    val Records: List<RequestRecord>
  );

  @Serializable
  private inner class RequestRecord(
    val id: Int,
    val data: T,
  );

  @Serializable
  private inner class ResponseRecord(
    val id: Int,
    val data: R?,
    val errorMessages: List<String>?
  );

  @Serializable
  private inner class ResponsePayload(
    val Records: List<ResponseRecord>
  );
}