package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckVerifiedClaimsSupported;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;

/**
 * Checks if the given authorization_request_object contains only expected claims
 */
public class AustraliaConnectIdEnsureVerifiedClaimsInRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {

		JsonElement requestedIdTokenClaimsEl = env.getElementFromObject("authorization_request_object", "claims.claims.id_token.verified_claims.claims");

		if (requestedIdTokenClaimsEl == null) {
			logSuccess("No id_token verified claims requested in request object.");
			return env;
		}

		JsonObject requestedIdTokenVerifiedClaims = requestedIdTokenClaimsEl.getAsJsonObject();

		ArrayList<String> unexpectedClaims = new ArrayList<>();

		for (String claim : requestedIdTokenVerifiedClaims.keySet()) {
			if (AustraliaConnectIdCheckVerifiedClaimsSupported.ConnectIdVerifiedClaims.contains(claim)) {
				continue;
			}

			unexpectedClaims.add(claim);
		}

		if (! unexpectedClaims.isEmpty()) {
			throw error("Unexpected id_token verified claims in authorization request object.",
				args("expected", AustraliaConnectIdCheckVerifiedClaimsSupported.ConnectIdVerifiedClaims, "actual", requestedIdTokenVerifiedClaims, "unexpected", unexpectedClaims));
		}

		logSuccess("All id_token verified claims in the authorization request object are expected.",
			args("expected", AustraliaConnectIdCheckVerifiedClaimsSupported.ConnectIdVerifiedClaims, "actual", requestedIdTokenVerifiedClaims));

		return env;
	}
}
