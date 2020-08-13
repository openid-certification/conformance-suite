package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAudToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		String serverIssuerUrl = env.getString("server", "issuer");

		if (serverIssuerUrl != null) {
			requestObjectClaims.addProperty("aud", serverIssuerUrl);

			env.putObject("request_object_claims", requestObjectClaims);

			logSuccess("Added aud to request object claims", args("aud", serverIssuerUrl));
		} else {
			// Only a "should" requirement
			log("Request object contains no audience and server issuer URL not found");
		}

		return env;
	}
}
