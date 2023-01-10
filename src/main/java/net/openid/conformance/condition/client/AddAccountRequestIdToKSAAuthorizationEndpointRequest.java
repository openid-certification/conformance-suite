package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAccountRequestIdToKSAAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "account_request_id", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		String consentid = env.getString("account_request_id");

		authorizationEndpointRequest.addProperty("scope", "accounts:" + consentid + " openid");

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
		return env;
	}


}
