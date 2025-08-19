package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OIDSSFOptionalFieldsTransmitterMetadataCheck extends AbstractCondition {

	private static final Set<String> OPTIONAL_FIELDS = Set.of(
		"jwks_uri",
		"delivery_methods_supported",
		"configuration_endpoint",
		"status_endpoint",
		"add_subject_endpoint",
		"remove_subject_endpoint",
		"verification_endpoint",
		"critical_subject_members",
		"authorization_schemes",
		"default_subjects"
	);

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf","transmitter_metadata").getAsJsonObject();

		Map<String, Object> optionalFields = new HashMap<>();
		for (var field : OPTIONAL_FIELDS) {
			if (transmitterMetadata.has(field)) {
				optionalFields.put(field, transmitterMetadata.get(field));
			}
		}

		if (optionalFields.isEmpty()) {
			log("No optional fields found");
		} else {
			log("Found optional fields", args("optional_fields", optionalFields.keySet()));
		}

		return env;
	}
}
