package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

/**
 * Records the subject identifier format used by the {@code sub_id} of the current SET under
 * {@code ssf.observed_subject_formats}, keyed by format, so that
 * {@link OIDSSFLogObservedSubjectFormats} can summarise which formats a transmitter used across
 * all received events.
 * <p>
 * Stored structure per format: {@code count}, {@code event_types} (distinct event type URIs)
 * and, for Complex Subjects, {@code members} (Subject Member name → distinct member formats).
 */
public class OIDSSFRecordSecurityEventTokenSubjectFormat extends AbstractCondition {

	public static final String OBSERVED_SUBJECT_FORMATS_KEY = "observed_subject_formats";

	@Override
	@PreEnvironment(required = {"set_token", "ssf"})
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getElementFromObject("set_token", "claims").getAsJsonObject();
		JsonElement subId = claims.get("sub_id");
		String format = SsfSubjectIdentifiers.getFormat(subId);
		if (format == null) {
			log("SET has no sub_id with a format member, nothing to record", args("sub_id", subId));
			return env;
		}

		JsonObject observed = getOrCreateObject(env.getObject("ssf"), OBSERVED_SUBJECT_FORMATS_KEY);
		JsonObject entry = getOrCreateObject(observed, format);

		int count = entry.has("count") ? OIDFJSON.getInt(entry.get("count")) : 0;
		entry.addProperty("count", count + 1);

		JsonArray eventTypes = getOrCreateArray(entry, "event_types");
		JsonObject events = claims.getAsJsonObject("events");
		if (events != null) {
			for (String eventType : events.keySet()) {
				addIfAbsent(eventTypes, eventType);
			}
		}

		if (SsfSubjectIdentifiers.isComplex(subId)) {
			JsonObject members = getOrCreateObject(entry, "members");
			for (Map.Entry<String, JsonElement> member : SsfSubjectIdentifiers.getComplexSubjectMembers(subId).entrySet()) {
				String memberFormat = SsfSubjectIdentifiers.getFormat(member.getValue());
				addIfAbsent(getOrCreateArray(members, member.getKey()), memberFormat == null ? "<missing format>" : memberFormat);
			}
		}

		// re-put so the change is visible regardless of how the ssf object is stored
		env.putObject("ssf", OBSERVED_SUBJECT_FORMATS_KEY, observed);

		logSuccess("Recorded subject identifier format of SET", args("format", format, "sub_id", subId, "event_types", eventTypes));

		return env;
	}

	private static JsonObject getOrCreateObject(JsonObject parent, String key) {
		JsonElement existing = parent.get(key);
		if (existing != null && existing.isJsonObject()) {
			return existing.getAsJsonObject();
		}
		JsonObject created = new JsonObject();
		parent.add(key, created);
		return created;
	}

	private static JsonArray getOrCreateArray(JsonObject parent, String key) {
		JsonElement existing = parent.get(key);
		if (existing != null && existing.isJsonArray()) {
			return existing.getAsJsonArray();
		}
		JsonArray created = new JsonArray();
		parent.add(key, created);
		return created;
	}

	private static void addIfAbsent(JsonArray array, String value) {
		for (JsonElement el : array) {
			if (value.equals(OIDFJSON.getString(el))) {
				return;
			}
		}
		array.add(value);
	}
}
