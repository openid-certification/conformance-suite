package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

//Sets the aud for Client Assertion to issuer url
//NOTE: this class assumes that the test has already invoked the condition: CreateClientAuthenticationAssertionClaims
public class UpdateClientAuthenticationAssertionClaimsWithISSAud extends AbstractCondition {


	@Override
	@PreEnvironment(required = "client_assertion_claims")
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("client_assertion_claims").getAsJsonObject();

		updateAudience(claims, env);

		logSuccess("Updated audience in client assertion claims", claims);

		env.putObject("client_assertion_claims", claims);

		return env;

	}

	private void updateAudience(JsonObject claims, Environment env) {

		claims.remove("aud");

		String audience = env.getString("server", "issuer");
		if (Strings.isNullOrEmpty(audience)) {
			throw error("Couldn't find required configuration element", args( "issuer", audience));
		}
		claims.addProperty("aud", audience);
	}
}
