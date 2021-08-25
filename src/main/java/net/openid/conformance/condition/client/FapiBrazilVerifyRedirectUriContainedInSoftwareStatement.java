package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FapiBrazilVerifyRedirectUriContainedInSoftwareStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "software_statement_assertion", strings = "redirect_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		// Note the non-RFC7591 claim software_redirect_uris that is used in Brazil & UK OpenBanking
		JsonElement redirectUris = env.getElementFromObject("software_statement_assertion", "claims.software_redirect_uris");

		if (redirectUris == null || !redirectUris.isJsonArray()) {
			throw error("software_redirect_uris is software statement is missing or not an array");
		}
		JsonArray redirectUrisArray = redirectUris.getAsJsonArray();

		String redirectUri = env.getString("redirect_uri");
		if (Strings.isNullOrEmpty(redirectUri)) {
			throw error("No redirect_uri found");
		}

		if (!redirectUrisArray.contains(new JsonPrimitive(redirectUri))) {
			throw error("The redirect_uri required for the conformance suite is not present in the software statement, registration must not succeed. Note that when the software statement contains multiple urls they MUST be specified as an array of strings. A single string containing comma separated URLs is not permitted by the specification.",
				args("required", redirectUri, "present", redirectUrisArray));
		}

		log("Required redirect_uri is present in the software statement",
			args("required", redirectUri, "present", redirectUrisArray));

		return env;
	}
}
