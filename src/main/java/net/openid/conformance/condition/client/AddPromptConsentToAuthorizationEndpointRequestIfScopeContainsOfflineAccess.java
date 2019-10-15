package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess extends AbstractCondition {

	/**
	 * Adds prompt=consent to authorization request only when scope contains offline_access
	 * @param env
	 * @return
	 */
	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env)
	{
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		if(!authorizationEndpointRequest.has("scope")) {
			logSuccess("Not adding prompt=consent as the authorization endpoint request does not contain a scope");
			return env;
		}
		String scope = OIDFJSON.getString(authorizationEndpointRequest.get("scope"));
		if(!scope.contains("offline_access")) {
			logSuccess("Not adding prompt=consent as the scope in the configuration does not contain offline_access");
			return env;
		}
		authorizationEndpointRequest.addProperty("prompt", "consent");
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
		logSuccess("Added prompt=consent to authorization endpoint request", authorizationEndpointRequest);
		return env;
	}
}
