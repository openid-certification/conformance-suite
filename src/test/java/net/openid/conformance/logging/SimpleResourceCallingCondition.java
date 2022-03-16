package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractCallProtectedResource;
import net.openid.conformance.testmodule.Environment;

public class SimpleResourceCallingCondition extends AbstractCallProtectedResource {

	@Override
	public Environment evaluate(Environment env) {
		callProtectedResource(env);
		return env;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		return env;
	}
}
