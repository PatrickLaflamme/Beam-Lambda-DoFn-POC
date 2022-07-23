import json

import falcon

from triton.lambdas import handler


class Resource:
    def on_post(self, req, resp, **params):
        context_body = req.headers.get("X-Amz-Client-Context")
        context = {
            "function_name": params.get("function_name")
        }
        if context_body:
            context.update(json.loads(context_body))
        body = req.stream.read()
        event = {}
        if body:
            event = json.loads(body)
        result = handler(event, context)
        # Create a JSON representation of the resource
        resp.text = json.dumps(result, ensure_ascii=False)

        # The following line can be omitted because 200 is the default
        # status returned by the framework, but it is included here to
        # illustrate how this may be overridden as needed.
        resp.status = falcon.HTTP_200


app = application = falcon.App()

resource = Resource()
app.add_route('/2015-03-31/functions/{function_name}/invocations', resource)