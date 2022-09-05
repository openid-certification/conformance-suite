package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class EnsureIdTokenContainsRequestedClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"id_token", "authorization_endpoint_request"} )
	public Environment evaluate(Environment env) {
		JsonObject requestedClaims = env.getElementFromObject("authorization_endpoint_request", "claims.id_token").getAsJsonObject();

		JsonObject idTokenClaims = env.getElementFromObject("id_token", "claims").getAsJsonObject();

		List<String> missingClaims = new ArrayList<>();

		for (String claim : requestedClaims.keySet()) {
			if (!idTokenClaims.has(claim)) {
				missingClaims.add(claim);
			}
		}

		if (missingClaims.isEmpty()) {
			logSuccess("id_token contains all the requested claims");
			return env;
		}

		throw error("id_token does not contain all the requested claims. Please check the test user contains the claims, that the server correctly understood the request, and that consent was granted to share the claims. It is permitted for the claims to be omitted from the id_token, as the server may have chosen to return them from the userinfo endpoint instead, in which case you may ignore this warning.",
			args("requested", requestedClaims, "missing", missingClaims));
	}

}
