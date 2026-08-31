package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrEnsureIntrospectionResponseDoesNotContainUsername extends AbstractCondition {

	@Override
	@PreEnvironment(required = CallTokenIntrospectionEndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {

		JsonElement username = env.getElementFromObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json.username");

		if (username != null) {
			throw error("The CDR standards do not allow the username claim in introspection responses",
				args("username", username));
		}

		logSuccess("The introspection response does not contain a username claim");
		return env;
	}

}
