package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureIdTokenDoesNotContainPersonalInformationClaims extends AbstractCondition {

	// The OpenID Connect standard claims from the 'profile', 'email', 'phone' and 'address'
	// scopes; CDR does not permit Personal Information claims in ID Tokens.
	private static final List<String> PI_CLAIMS = List.of(
		"name", "given_name", "family_name", "middle_name", "nickname", "preferred_username",
		"profile", "picture", "website", "gender", "birthdate", "zoneinfo", "locale",
		"email", "email_verified", "phone_number", "phone_number_verified", "address");

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getElementFromObject("id_token", "claims").getAsJsonObject();

		List<String> found = PI_CLAIMS.stream().filter(claims::has).toList();

		if (!found.isEmpty()) {
			throw error("CDR does not permit ID Tokens to contain Personal Information claims",
				args("personal_information_claims_found", found));
		}

		logSuccess("id_token does not contain any Personal Information claims");
		return env;
	}

}
