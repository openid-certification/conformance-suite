package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddMultipleAudToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.remove("aud");

		String serverIssuerUrl = env.getString("server", "issuer");
		JsonArray aud = new JsonArray();
		if (serverIssuerUrl != null) {
			aud.add(serverIssuerUrl);
		}
		aud.add("https://other1.example.com");
		aud.add("invalid");

		requestObjectClaims.add("aud", aud);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added multiple aud to request object claims", args(
			"aud", requestObjectClaims.getAsJsonArray("aud"))
		);
		return env;
	}
}
