package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckRequestObjectClaimsParameterValues extends AbstractCondition {
	// https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
	private static final List<String> expectedTopLevelClaims = List.of(
		"userinfo",
		"id_token"
	);

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {

		List<String> invalidClaims = new ArrayList<>();
		List<String> validClaims = new ArrayList<>();

		JsonObject claimsParameter = env.getElementFromObject("authorization_request_object", "claims.claims").getAsJsonObject();

		if (claimsParameter == null || claimsParameter.size() == 0) {
			logSuccess("authorization_request_object.claims.claims does not exist or is empty");
			return env;
		}

		for (String claim : claimsParameter.keySet()) {
			// Ignore unexpected claims
			if (! expectedTopLevelClaims.contains(claim)) {
				continue;
			}

			JsonElement claimObject = claimsParameter.get(claim);

			if (claimObject instanceof JsonObject) {
				validClaims.add(claim);
			}
			else {
				invalidClaims.add(claim);
			}
		}

		if (invalidClaims.isEmpty()) {
			logSuccess("the expected authorization_request_object.claims.claims claims values are json objects", args("valid_claims", validClaims));
		} else {
			throw error("the expected authorization_request_object.claims.claims claims values are not json objects", args("valid_claims", validClaims, "invalid_claims", invalidClaims));
		}

		return env;
	}
}
