package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveRedirectUriFromDynamicClientRegistrationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dynamic_registration_request"})
	public Environment evaluate(Environment env) {
		JsonObject req = env.getObject("dynamic_registration_request");

		req.remove("redirect_uri");

		return env;
	}

}
