package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ExpectRedirectUriHasBeenCalled extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");
		if (authorizationEndpointResponse == null) {
			throw error("The OpenID Connect core specification states that 'Authorization Servers MUST support the use " +
				"of the HTTP GET and POST methods defined in RFC 7231 at the Authorization Endpoint'. In the " +
				"conformance suite, failure to correctly respond to an HTTP POST request to the authorization endpoint " +
				"within 30 seconds is considered a WARNING.");
		}

		return env;
	}

}
