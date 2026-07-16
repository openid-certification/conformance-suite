package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilAddSoftwareStatementRedirectUrisToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "software_statement_assertion" })
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement redirectUrisElement = env.getElementFromObject(
			"software_statement_assertion", "claims.software_redirect_uris");
		if (redirectUrisElement == null) {
			throw error("Software statement must contain software_redirect_uris for Open Finance Brazil dynamic registration");
		}
		if (!redirectUrisElement.isJsonArray() || redirectUrisElement.getAsJsonArray().isEmpty()) {
			throw error("Software statement software_redirect_uris must be a non-empty array",
				args("software_redirect_uris", redirectUrisElement));
		}

		JsonArray redirectUris = redirectUrisElement.getAsJsonArray();
		for (JsonElement redirectUriElement : redirectUris) {
			if (!redirectUriElement.isJsonPrimitive()
				|| !redirectUriElement.getAsJsonPrimitive().isString()
				|| Strings.isNullOrEmpty(OIDFJSON.getString(redirectUriElement))) {
				throw error("Software statement software_redirect_uris must contain only non-empty strings",
					args("software_redirect_uris", redirectUris));
			}
		}

		JsonObject registrationRequest = env.getObject("dynamic_registration_request");
		registrationRequest.add("redirect_uris", redirectUris.deepCopy());

		logSuccess("Added redirect_uris from the software statement to the dynamic registration request",
			args("redirect_uris", redirectUris));
		return env;
	}
}
