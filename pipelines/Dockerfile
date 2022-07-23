FROM bde2020/spark-submit:3.1.1-hadoop3.2-java11

ENV SPARK_MASTER_NAME=spark-master-0.spark-headless.poc.svc.cluster.local \
    SPARK_MASTER_PORT=7077 \
    SPARK_APPLICATION_JAR_LOCATION=pipelines.shadow.jar \
    SPARK_APPLICATION_MAIN_CLASS=ai.triton.platform.AppKt \
    SPARK_APPLICATION_ARGS="--runner=SparkRunner"

COPY build/libs/pipelines.shadow.jar pipelines.shadow.jar

ENTRYPOINT [ "./submit.sh" ]