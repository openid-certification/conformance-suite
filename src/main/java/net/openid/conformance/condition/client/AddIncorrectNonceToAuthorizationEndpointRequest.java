package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class AddIncorrectNonceToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String nonce = RandomStringUtils.secure().nextAlphanumeric(10);

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("nonce", nonce);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added incorrect nonce parameter to request", authorizationEndpointRequest);

		return env;

	}
}
