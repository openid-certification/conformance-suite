package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class OIDSSFStreamRequiredFieldsCheck extends AbstractCondition {

	protected final Set<String> REQUIRED_FIELDS = Set.of(
		"stream_id",
		"iss",
		"aud",
		"events_delivered",
		"delivery"
	);

	@Override
	public Environment evaluate(Environment env) {

		JsonElement streamEl = env.getElementFromObject("ssf", "stream");
		if (streamEl == null) {
			throw error("ssf stream element not found");
		}

		JsonObject stream = streamEl.getAsJsonObject();

		Map<String, Object> requiredFields = new HashMap<>();
		for (var field : REQUIRED_FIELDS) {
			if (stream.has(field)) {
				requiredFields.put(field, stream.get(field));
			}
		}

		Set<String> missingFields = new TreeSet<>(REQUIRED_FIELDS);
		missingFields.removeAll(requiredFields.keySet());

		if (!missingFields.isEmpty()) {
			throw error("Missing required fields in stream configuration", args("required_fields", REQUIRED_FIELDS, "missing_fields", missingFields));
		}

		logSuccess("Found all required fields in stream configuration", args("required_fields", requiredFields.keySet()));

		return env;
	}
}
