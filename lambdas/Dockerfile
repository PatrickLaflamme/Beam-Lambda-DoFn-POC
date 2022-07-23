FROM public.ecr.aws/lambda/python:3.8

# Copy function code
COPY triton ${LAMBDA_TASK_ROOT}/triton

# Install the function's dependencies using file requirements.txt
# from your project folder.
#
RUN pip3 install gunicorn
COPY requirements.txt  .
RUN  pip3 install -r requirements.txt --target "${LAMBDA_TASK_ROOT}"

EXPOSE 8080
EXPOSE 8081

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
ENTRYPOINT [ "/usr/bin/sh", "-c", "gunicorn -b 0.0.0.0:8081 triton.web & /lambda-entrypoint.sh triton.lambdas.handler" ]