from random import random


def handler(event, context):
    responses = []
    for record in event.get("Records", []):
        current = {k: v for k, v in record.get("data").items() if k != "previous"}
        if random() > 0.1:
            responses.append({
                "id": record.get("id"),
                "data": {
                    "previous": record.get("data", {}).get("previous", []) + [current],
                    "current": {
                        "functionName": context.get("function_name"),
                        "awsRequestId": context.get("aws_request_id")
                    }
                }
            })
        else:
            responses.append({
                "id": record.get("id"),
                "errorMessages": [
                    "random number was too small!",
                    "some other issue",
                    "etc..."
                ]
            })
    return {
        "Records": responses
    }
