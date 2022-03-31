package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class CheckIfOidcStandardClaimsSupported extends AbstractCondition {

	public static String envVarAtLeastOneOidcStandardClaimSupport = "at_least_one_oidc_standard_claim_supported";

	private static final String environmentVariable = "claims_supported";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement claimsSupportedEl = env.getElementFromObject("server", environmentVariable);

		env.putBoolean(envVarAtLeastOneOidcStandardClaimSupport, false);

		if (claimsSupportedEl == null) {
			log(environmentVariable + " not found in server metadata");
			return env;
		}

		if (!claimsSupportedEl.isJsonArray()) {
			throw error(environmentVariable + " in server metadata is not an array", args(environmentVariable, claimsSupportedEl));
		}

		JsonArray claimsSup = claimsSupportedEl.getAsJsonArray();
		Set<String> claimsSupported = new HashSet<>();
		for (JsonElement el : claimsSup) {
			if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
				throw error(environmentVariable + " in server metadata is not an array", args(environmentVariable, claimsSupportedEl));
			}
			claimsSupported.add(OIDFJSON.getString(el));
		}

		claimsSupported.retainAll(AddAllSupportedStandardClaimsToAuthorizationEndpointRequestIdTokenAndUserinfoClaims.oidcClaims);

		if (claimsSupported.isEmpty()) {
			log(environmentVariable + " in server metadata does not contain any of the OpenID Connect standard claims");
			return env;
		}

		env.putBoolean(envVarAtLeastOneOidcStandardClaimSupport, true);

		log(environmentVariable + " in server metadata contains at least one OpenID Connect standard claims",
			args("standard_claims", claimsSupported));
		return env;
	}

}
