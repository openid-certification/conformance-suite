package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class ValidateIdTokenStandardClaims extends AbstractValidateOpenIdStandardClaims {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		JsonObject idTokenClaims = env.getElementFromObject("id_token", "claims").getAsJsonObject().deepCopy();

		List<String> idTokenNonIdentityClaims = List.of(
			// as per https://openid.net/specs/openid-connect-core-1_0.html#IDToken
			"iss",
			// "sub" - leave sub in, it's present in userinfo too
			"aud",
			"exp",
			"iat",
			"auth_time",
			"nonce",
			"acr",
			"amr",
			"azp",
			// as per https://openid.net/specs/openid-connect-core-1_0.html#HybridIDToken
			"c_hash",
			"at_hash",
			// from FAPI standard
			"s_hash"
		);

		for (String e : idTokenNonIdentityClaims) {
			// remove the claims that are specific to the id_token, so we're left with just claims from
			// https://openid.net/specs/openid-connect-core-1_0.html#Claims
			// (these id_token claims are mostly checked in other conditions, ValidateIdToken
			// and the various validations of the hashes)
			idTokenClaims.remove(e);
		}

		if (new ObjectValidator(null, STANDARD_CLAIMS).isValid(idTokenClaims)) {
			logSuccess("id_token claims are valid");
		} else {
			throw error("id_token claims are not valid", idTokenClaims);
		}

		return env;
	}

}
