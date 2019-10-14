package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientAuthenticationAudIssuerIdentifierToBackchannelAuthenticationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client_assertion_claims", "server" })
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("client_assertion_claims");

		String aud = env.getString("server", "issuer");

		claims.addProperty("aud", aud);

		env.putObject("client_assertion_claims", claims);

		logSuccess("Add Issuer Identifier as aud value to client_assertion_claims", claims);

		return env;
	}
}
