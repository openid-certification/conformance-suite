package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFCreateStreamConfigCall extends AbstractOIDSSFStreamConfigCall {

	@Override
	protected String getEndpointName() {
		return "stream configuration";
	}

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "POST");
		addResourceRequestEntity(env);
	}

	protected void addResourceRequestEntity(Environment env) {
		env.putString("resource_request_entity", createResourceRequestEntityString(env));
	}

	protected String createResourceRequestEntityString(Environment env) {
		return new Gson().toJson(createResourceRequestEntity(env));
	}

	protected JsonObject createResourceRequestEntity(Environment env) {
		return env.getElementFromObject("ssf", "stream.config").getAsJsonObject();
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		JsonObject streamConfigObject = env.getElementFromObject("resource_endpoint_response_full", "body_json").getAsJsonObject();
		env.putObject("ssf", "stream", streamConfigObject);
		return env;
	}
}
