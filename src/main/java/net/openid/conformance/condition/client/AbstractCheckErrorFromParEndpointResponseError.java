package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractCheckErrorFromParEndpointResponseError extends AbstractCheckErrorFromResponseError {

	@Override
	protected String getResponseKey() {
		return CallPAREndpoint.RESPONSE_KEY;
	}

	@Override
	@PreEnvironment(required = CallPAREndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected String getError(Environment env) {
		JsonObject resp = env.getElementFromObject(CallPAREndpoint.RESPONSE_KEY, "body_json").getAsJsonObject();

		JsonPrimitive error = resp.getAsJsonPrimitive("error");
		if (error == null) {
			throw error("Expected 'error' field is not present in PAR response");
		}
		return OIDFJSON.getString(error);
	}
}
