package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetClientAuthenticationAudTokenEndpointToBackchannelAuthenticationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client_assertion_claims", "server" })
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("client_assertion_claims");

		String aud = env.getString("server", "token_endpoint");

		claims.addProperty("aud", aud);

		env.putObject("client_assertion_claims", claims);

		logSuccess("Add token_endpoint as aud value to client_assertion_claims", claims);

		return env;
	}
}
