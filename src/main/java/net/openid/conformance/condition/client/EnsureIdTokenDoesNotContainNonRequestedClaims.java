package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnsureIdTokenDoesNotContainNonRequestedClaims extends AbstractVerifyScopesReturnedInClaims {
	public static List<String> idTokenValidClaims = new ArrayList<>(Arrays.asList(
		// as per https://openid.net/specs/openid-connect-core-1_0.html#IDToken
		"iss",
		"sub",
		"aud",
		"exp",
		"iat",
		"auth_time",
		"nonce",
		"acr",
		"amr",
		"azp",
		// as per https://openid.net/specs/openid-connect-core-1_0.html#CodeIDToken
		"at_hash",
		// as per https://openid.net/specs/openid-financial-api-part-2-1_0.html#id-token-as-detached-signature
		"s_hash"
	));

	@Override
	@PreEnvironment(required = {"id_token", "authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		// For OpenID Connect, scopes can be used to request that specific sets of information be made available
		// as claim values.
		//
		// Here we map the scopes included in the authorization request to a set of claims and add them to the
		// valid claims list.
		String scopeStr = env.getString("authorization_endpoint_request", "scope");

		if (! Strings.isNullOrEmpty(scopeStr)) {
			List<String> scopeList = Arrays.asList(scopeStr.split(" "));

			if (scopeList.contains("openid")) {
				for (String scope: scopeList) {
					List<String> claimsList = SCOPE_STANDARD_CLAIMS.get(scope);

					if (claimsList == null) {
						continue;
					}

					for (String claim: claimsList) {
						if (! idTokenValidClaims.contains(claim)) {
							idTokenValidClaims.add(claim);
						}
					}
				}
			}
		}

		JsonObject idTokenClaims = env.getElementFromObject("id_token", "claims").getAsJsonObject().deepCopy();

		boolean failure = false;

		for (String key : idTokenClaims.keySet()) {
			if (idTokenValidClaims.contains(key)) {
				continue;
			}

			failure = true;

			logFailure("id_token contains non-requested claim '" + key + "'");
		}

		if (failure) {
			throw error("id_token contains non-requested claims");
		}

		logSuccess("no non-requested id_token claims found");

		return env;
	}
}
