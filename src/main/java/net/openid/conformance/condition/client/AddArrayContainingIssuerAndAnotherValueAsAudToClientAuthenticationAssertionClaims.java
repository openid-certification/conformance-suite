package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;


//Sets the Token Endpoint URL as the Audience claim for Client Authentication Assertion.
public class AddArrayContainingIssuerAndAnotherValueAsAudToClientAuthenticationAssertionClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client_assertion_claims", "server" })
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("client_assertion_claims").getAsJsonObject();

		String audience = env.getString("server", "issuer");
		JsonArray aud = new JsonArray();
		aud.add(audience);

		audience = env.getString("token_endpoint") != null ?
			env.getString("token_endpoint") : env.getString("server", "token_endpoint");

		if (Strings.isNullOrEmpty(audience)) {
			throw error("Couldn't find required configuration element", args("audience", audience));
		}

		aud.add(audience);
		claims.add("aud", aud);

		logSuccess("Set audience in client assertion claims to be an array containing the issuer and another value", claims);

		env.putObject("client_assertion_claims", claims);

		return env;
	}

}
