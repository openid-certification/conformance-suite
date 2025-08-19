package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class OIDSSFStreamOptionalFieldsCheck extends AbstractCondition {

	protected final Set<String> OPTIONAL_FIELDS = Set.of(
		"events_supported",
		"events_requested",
		"min_verification_interval",
		"description",
		"inactivity_timeout"
	);

	@Override
	public Environment evaluate(Environment env) {

		JsonElement streamEl = env.getElementFromObject("ssf", "stream");
		if (streamEl == null) {
			throw error("ssf stream element not found");
		}

		JsonObject stream = streamEl.getAsJsonObject();

		Map<String, Object> optionalFields = new TreeMap<>();
		for (var field : OPTIONAL_FIELDS) {
			if (stream.has(field)) {
				optionalFields.put(field, stream.get(field));
			}
		}

		if (optionalFields.isEmpty()) {
			log("Found no optional fields in stream configuration");
		} else {
			log("Found optional fields in stream configuration", args("optional_fields", optionalFields.keySet()));
		}

		return env;
	}
}
