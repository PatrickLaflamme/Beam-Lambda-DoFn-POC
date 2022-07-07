FROM bde2020/spark-submit:3.3.0-hadoop3.3

ENV SPARK_MASTER_NAME=spark-master-svc \
    SPARK_MASTER_PORT=7077 \
    SPARK_APPLICATION_JAR_LOCATION=pipelines.shadow.jar \
    SPARK_APPLICATION_MAIN_CLASS=ai.triton.platform.AppKt \
    SPARK_APPLICATION_ARGS=""

COPY build/libs/pipelines.shadow.jar pipelines.shadow.jar

ENTRYPOINT [ "./submit.sh" ]