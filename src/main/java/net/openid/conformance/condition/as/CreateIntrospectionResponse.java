package net.openid.conformance.condition.as;

import java.time.Instant;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateIntrospectionResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"access_token", "client_id"}, required = {"introspection_request", "resource"})
	@PostEnvironment(required = "introspection_response")
	public Environment evaluate(Environment env) {

		// get the access token being introspected
		String introspectedToken = env.getString("introspection_request", "params.token");

		if (Strings.isNullOrEmpty(introspectedToken)) {
			throw error("Couldn't find token to introspect");
		}

		// get the token we issued
		String accessToken = env.getString("access_token");

		// get the scopes the resource is looking for
		String scope = env.getString("resource", "scope");

		// get the client ID
		String clientId = env.getString("client_id");

		// Set an expiration sometime in the future
		Instant exp = Instant.now().plusSeconds(600);

		// compare the token to the one we issued
		if (introspectedToken.equals(accessToken)) {
			// the tokens match, prepare the results

			JsonObject res = new JsonObject();
			res.addProperty("active", true);
			res.addProperty("scope", scope);
			res.addProperty("client_id", clientId);
			res.addProperty("exp", exp.getEpochSecond());


			env.putObject("introspection_response", res);

			logSuccess("Created introspection response", res);

			return env;
		} else {
			// if we don't find the token, it's not a failure in the test framework, but we do return an introspection response
			JsonObject res = new JsonObject();
			res.addProperty("active", false);

			logFailure("Introspected token not found.", args("expected", accessToken, "actual", introspectedToken, "introspection_response", res));

			env.putObject("introspection_response", res);

			return env;

		}

	}

}
