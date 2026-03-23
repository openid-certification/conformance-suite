package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckForUnexpectedClaimsInClaimsParameter extends AbstractCondition {
	public static List<String> expectedClaims = List.of(
		// as per https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
		"userinfo",
		"id_token"
	);

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		JsonObject claimsParameter = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "claims").getAsJsonObject();
		List<String> unknownClaims = new ArrayList<>();

		if (claimsParameter == null || claimsParameter.size() == 0) {
			logSuccess("authorization request 'claims' parameter does not exist or is empty");
			return env;
		}

		for (String claim : claimsParameter.keySet()) {
			if (expectedClaims.contains(claim)) {
				continue;
			}

			unknownClaims.add(claim);
		}

		if (unknownClaims.isEmpty()) {
			logSuccess("authorization request 'claims' parameter contains only expected claims", args("claims", claimsParameter.keySet()));
		} else {
			throw error("unknown claims found in authorization request 'claims' parameter", args("claims", claimsParameter.keySet(), "unknown_claims", unknownClaims));
		}

		return env;
	}
}
