package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureIdTokenDoesNotContainNonRequestedClaims extends AbstractCondition {
	public static List<String> idTokenValidClaims = List.of(
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
	);

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

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
