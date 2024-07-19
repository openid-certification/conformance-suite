package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Checks if the given authorization_request_object contains the expectzed claims
 */
public class AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject extends AbstractCondition {

	private static final Set<String> EXPECTED_REQUESTED_CLAIMS = Set.of(
		// as per https://cdn.connectid.com.au/specifications/digitalid-identity-assurance-profile-06.html#section-5.2-2.7
		"txn"
	);

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getElementFromObject("authorization_request_object", "claims").getAsJsonObject();

		Set<String> missingExpectedClaims = new LinkedHashSet<>();
		for (String expectedClaim : EXPECTED_REQUESTED_CLAIMS) {
			if (!requestObjectClaims.has(expectedClaim)) {
				missingExpectedClaims.add(expectedClaim);
			}
		}

		if (!missingExpectedClaims.isEmpty()) {
			throw error("Missing expected claims in authorization_request_object.", args("missing_expected_claims", missingExpectedClaims));
		}

		logSuccess("Expected claims are present in authorization_request_object.", args("expected_claims", EXPECTED_REQUESTED_CLAIMS));

		return env;
	}
}
