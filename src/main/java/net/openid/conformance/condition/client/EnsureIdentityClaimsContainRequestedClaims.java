package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class EnsureIdentityClaimsContainRequestedClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"identity_claims", "authorization_endpoint_request"} )
	public Environment evaluate(Environment env) {
		// assume we requested same claims in id_token and userinfo, as per
		// AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenAndUserinfoClaims
		// (but deliberately use userinfo, as none-identity claims like acr might have been requested for the id_token)
		JsonObject requestedClaims = env.getElementFromObject("authorization_endpoint_request", "claims.userinfo").getAsJsonObject();

		JsonObject identityClaims = env.getObject("identity_claims");

		List<String> missingClaims = new ArrayList<>();

		for (String claim : requestedClaims.keySet()) {
			if (!identityClaims.has(claim)) {
				missingClaims.add(claim);
			}
		}

		if (missingClaims.isEmpty()) {
			logSuccess("id_token and userinfo combined contain all the requested claims");
			return env;
		}

		throw error("The server did not return all the requested claims. Please check the test user contains the claims, that the server correctly understood the request, and that consent was granted to share the claims. As the server listed the claims in claims_supported, it should have returned them in either the id_token or the userinfo response.",
			args("requested", requestedClaims, "returned", identityClaims, "missing", missingClaims));
	}

}
