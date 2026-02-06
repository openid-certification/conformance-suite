package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;
import java.util.TreeSet;

public class VCICheckForUnknownFieldsInNotificationRequest extends AbstractCondition {

	private static final Set<String> KNOWN_FIELDS = Set.of(
		"notification_id",
		"event",
		"event_description"
	);

	@Override
	public Environment evaluate(Environment env) {

		JsonElement bodyJsonEl = env.getElementFromObject("incoming_request", "body_json");

		if (bodyJsonEl == null || !bodyJsonEl.isJsonObject()) {
			log("Notification request did not contain a request body.");
			return env;
		}

		JsonObject body = bodyJsonEl.getAsJsonObject();

		Set<String> unknownFields = new TreeSet<>();
		for (String key : body.keySet()) {
			if (!KNOWN_FIELDS.contains(key)) {
				unknownFields.add(key);
			}
		}

		if (unknownFields.isEmpty()) {
			logSuccess("Found no unknown fields in notification request");
		} else {
			throw error("Found unknown fields in notification request", args("unknownFields", unknownFields));
		}

		return env;
	}
}
