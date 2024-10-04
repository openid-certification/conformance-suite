package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class OIDSSFRequiredFieldsTransmitterMetadataCheck extends AbstractCondition {

	private static final Set<String> REQUIRED_FIELDS = Set.of(
		"issuer",
		"authorization_schemes"
	);

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf","transmitter_metadata").getAsJsonObject();
		Set<String> missingFields = REQUIRED_FIELDS.stream().filter(field -> !transmitterMetadata.has(field)).collect(Collectors.toCollection(LinkedHashSet::new));

		if (!missingFields.isEmpty()) {
			throw error("Couldn't find required fields in transmitter_metadata", args("missing_fields", missingFields));
		}

		logSuccess("Found all required fields");

		return env;
	}
}
