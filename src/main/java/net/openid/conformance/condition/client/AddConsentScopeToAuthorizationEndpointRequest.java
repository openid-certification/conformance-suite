package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddConsentScopeToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "consent_id", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String consentId = env.getString("consent_id");

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String scope = OIDFJSON.getString(authorizationEndpointRequest.get("scope"));

		scope += " consent:"+consentId;

		authorizationEndpointRequest.addProperty("scope", scope);

		logSuccess("Added consent scope to authorization_endpoint_request", args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}

}
