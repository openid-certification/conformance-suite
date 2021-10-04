package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.client.AbstractCallProtectedResource;
import net.openid.conformance.testmodule.Environment;
import com.google.gson.JsonObject;

public class CallResource extends AbstractCallProtectedResource {

	@Override
	public Environment evaluate(Environment env) {
		return callProtectedResource(env);
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		env.putString("resource_endpoint_response", responseBody);
		logSuccess("Got a response from the resource endpoint: ", fullResponse);
		return env;
	}
}
