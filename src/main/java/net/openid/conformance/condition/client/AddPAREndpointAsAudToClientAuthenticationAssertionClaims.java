package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;


//Sets the Pushed Authorization Request Endpoint URL as the Audience claim for Client Authentication Assertion.
public class AddPAREndpointAsAudToClientAuthenticationAssertionClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_assertion_claims")
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("client_assertion_claims").getAsJsonObject();

		addAudience(claims, env);

		logSuccess("add audience in client assertion claims", claims);

		env.putObject("client_assertion_claims", claims);

		return env;
	}

	private void addAudience(JsonObject claims, Environment env) {
		String audience = env.getString("server", "pushed_authorization_request_endpoint");
		if (Strings.isNullOrEmpty(audience)) {
			throw error("Couldn't find required configuration element", args( "audience", audience));
		}
		claims.addProperty("aud", audience);
	}
}
