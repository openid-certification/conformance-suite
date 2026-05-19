package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddVerifierInfoToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { ExtractVerifierInfoFromClientConfiguration.ENV_KEY, "authorization_endpoint_request" })
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject wrapper = env.getObject(ExtractVerifierInfoFromClientConfiguration.ENV_KEY);
		JsonElement verifierInfo = wrapper.get(ExtractVerifierInfoFromClientConfiguration.WRAPPER_PROPERTY);

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		authorizationEndpointRequest.add("verifier_info", verifierInfo);

		logSuccess("Added verifier_info parameter to request", authorizationEndpointRequest);

		return env;
	}
}
