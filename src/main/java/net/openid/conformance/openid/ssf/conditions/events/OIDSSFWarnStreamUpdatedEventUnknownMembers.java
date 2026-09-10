package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;
import java.util.TreeSet;

/**
 * Flags members of a Stream Updated event object that
 * <a href="https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-8.1.5">SSF 1.0 Section 8.1.5</a>
 * does not define; the event "contains the following claims": {@code status} and {@code reason}. Additional fields
 * are permitted by SSF 1.0 Section 4.2.3 ("Transmitters MAY include additional fields in SSF events"), so callers
 * should invoke this at WARNING severity per the suite's unknown-property convention; such members often indicate
 * a misspelled member name.
 * <p>
 * Reads the parsed SET from {@code set_token.claims}. Structural validity is checked separately by
 * {@link OIDSSFValidateStreamUpdatedEvent}.
 */
public class OIDSSFWarnStreamUpdatedEventUnknownMembers extends AbstractCondition {

	private static final Set<String> KNOWN_MEMBERS = Set.of("status", "reason");

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getElementFromObject("set_token", "claims").getAsJsonObject();

		JsonElement events = claims.get("events");
		if (events == null || !events.isJsonObject()) {
			log("SET has no 'events' object, nothing to check", args("claims", claims));
			return env;
		}
		JsonElement event = events.getAsJsonObject().get(SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE);
		if (event == null || !event.isJsonObject()) {
			log("SET has no stream-updated event object, nothing to check", args("claims", claims));
			return env;
		}

		Set<String> unknownMembers = new TreeSet<>(event.getAsJsonObject().keySet());
		unknownMembers.removeAll(KNOWN_MEMBERS);
		if (!unknownMembers.isEmpty()) {
			throw error("The stream-updated event contains members the stream-updated event definition does not define. "
					+ "This may indicate a misspelled member name.",
				args("event", event, "unknown_members", unknownMembers, "known_members", KNOWN_MEMBERS));
		}

		logSuccess("The stream-updated event contains only the members the stream-updated event definition defines",
			args("event", event));

		return env;
	}
}
