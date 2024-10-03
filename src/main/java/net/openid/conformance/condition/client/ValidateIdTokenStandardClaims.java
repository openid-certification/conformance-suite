package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class ValidateIdTokenStandardClaims extends AbstractValidateOpenIdStandardClaims {
	public static List<String> idTokenNonIdentityClaims = List.of(
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
		"s_hash",
		// standard jwt claims https://datatracker.ietf.org/doc/html/rfc7519#section-4.1
		"jti",
		"nbf",
		// as per https://openid.net/specs/openid-connect-4-identity-assurance-1_0-ID3.html#name-verified_claims-element
		"verified_claims"
	);

	public static JsonObject getIdTokenIdentityClaims(Environment env) {
		JsonObject idTokenClaims = env.getElementFromObject("id_token", "claims").getAsJsonObject().deepCopy();

		for (String e : idTokenNonIdentityClaims) {
			// remove the claims that are specific to the id_token, so we're left with just claims from
			// https://openid.net/specs/openid-connect-core-1_0.html#Claims
			// (these id_token claims are mostly checked in other conditions, ValidateIdToken
			// and the various validations of the hashes)
			idTokenClaims.remove(e);
		}
		return idTokenClaims;
	}

	@Override
	@PreEnvironment(required = "id_token")
	@PostEnvironment(required = "id_token_unknown_claims")
	public Environment evaluate(Environment env) {

		JsonObject idTokenClaims = getIdTokenIdentityClaims(env);

		boolean result = new ObjectValidator(null, STANDARD_CLAIMS).isValid(idTokenClaims);
		env.putObject("id_token_unknown_claims", unknownClaims);
		if (result) {
			logSuccess("id_token claims are valid");
		} else {
			throw error("id_token claims are not valid", idTokenClaims);
		}

		return env;
	}

}
