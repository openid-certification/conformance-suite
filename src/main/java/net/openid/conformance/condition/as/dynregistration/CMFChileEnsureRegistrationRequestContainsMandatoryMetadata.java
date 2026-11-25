package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the registration metadata that CMF DCR section 6 marks as mandatory. software_statement is
 * checked with the software statement, and contacts by EnsureRegistrationRequestContainsAtLeastOneContact.
 */
public class CMFChileEnsureRegistrationRequestContainsMandatoryMetadata extends AbstractCondition {

	private static final List<String> MANDATORY_STRINGS = List.of(
		"token_endpoint_auth_method", "jwks_uri", "scope", "application_type", "client_name");

	private static final List<String> MANDATORY_ARRAYS = List.of("redirect_uris", "grant_types");

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("dynamic_registration_request");
		List<String> missing = new ArrayList<>();
		List<String> invalid = new ArrayList<>();

		for (String name : MANDATORY_STRINGS) {
			JsonElement value = request.get(name);
			if (value == null || value.isJsonNull()) {
				missing.add(name);
			} else if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
				invalid.add(name);
			}
		}
		for (String name : MANDATORY_ARRAYS) {
			JsonElement value = request.get(name);
			if (value == null || value.isJsonNull()) {
				missing.add(name);
			} else if (!value.isJsonArray() || value.getAsJsonArray().isEmpty()) {
				invalid.add(name);
			}
		}

		if (!missing.isEmpty() || !invalid.isEmpty()) {
			throw error("The registration request does not contain all the metadata that CMF requires",
				args("missing", missing, "invalid", invalid,
					"expected_strings", MANDATORY_STRINGS, "expected_non_empty_arrays", MANDATORY_ARRAYS));
		}
		logSuccess("The registration request contains all the metadata that CMF requires",
			args("strings", MANDATORY_STRINGS, "arrays", MANDATORY_ARRAYS));
		return env;
	}
}
