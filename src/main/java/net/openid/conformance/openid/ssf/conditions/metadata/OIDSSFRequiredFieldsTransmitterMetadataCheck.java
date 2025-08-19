package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OIDSSFRequiredFieldsTransmitterMetadataCheck extends AbstractCondition {

	private static final Set<String> REQUIRED_FIELDS = Set.of(
		"issuer",
		"authorization_schemes"
	);

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf","transmitter_metadata").getAsJsonObject();

		Map<String, Object> requiredFields = new HashMap<>();
		for (var field : REQUIRED_FIELDS) {
			if (transmitterMetadata.has(field)) {
				requiredFields.put(field, transmitterMetadata.get(field));
			}
		}

		Set<String> missingFields = new HashSet<>(REQUIRED_FIELDS);
		missingFields.removeAll(requiredFields.keySet());
		if (!missingFields.isEmpty()) {
			throw error("Couldn't find required fields in transmitter_metadata", args("missing_fields", missingFields));
		}

		logSuccess("Found all required fields", args("required_fields", requiredFields.keySet()));

		return env;
	}
}
