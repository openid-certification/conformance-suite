package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

/**
 * Extracts the CAEP event type and event data from a parsed SET token.
 * Reads from {@code set_token.claims.events} and stores:
 * <ul>
 *   <li>{@code ssf.caep_event.type} - the CAEP event type URI (null if not a CAEP event)</li>
 *   <li>{@code ssf.caep_event.data} - the event payload object</li>
 * </ul>
 * SSF framework events (e.g. verification, stream-updated) are silently skipped
 * since they are not CAEP events but are valid SET tokens.
 */
public class OIDSSFExtractCaepEventData extends AbstractCondition {

	@PreEnvironment(required = {"set_token", "ssf"})
	@Override
	public Environment evaluate(Environment env) {

		// Clear any previous CAEP event data
		JsonObject ssf = env.getObject("ssf");
		ssf.remove("caep_event");

		JsonObject claims = env.getElementFromObject("set_token", "claims").getAsJsonObject();
		JsonObject events = claims.getAsJsonObject("events");
		if (events == null || events.isEmpty()) {
			throw error("SET token does not contain an 'events' claim or it is empty", args("claims", claims));
		}

		for (Map.Entry<String, JsonElement> entry : events.entrySet()) {
			String eventType = entry.getKey();
			if (SsfEvents.CAEP_EVENT_TYPES.contains(eventType)) {
				JsonObject eventData = entry.getValue().getAsJsonObject();
				env.putString("ssf", "caep_event.type", eventType);
				env.putObject("ssf", "caep_event.data", eventData);
				logSuccess("Extracted CAEP event", args("event_type", eventType, "event_data", eventData));
				return env;
			}
			if (SsfEvents.SSF_EVENT_TYPES.contains(eventType)) {
				log("Skipping SSF framework event", args("event_type", eventType));
				return env;
			}
		}

		Set<String> foundEventTypes = events.keySet();
		throw error("SET token does not contain a recognized CAEP or SSF event type",
			args("found_event_types", foundEventTypes, "expected_caep", SsfEvents.CAEP_EVENT_TYPES));
	}
}
