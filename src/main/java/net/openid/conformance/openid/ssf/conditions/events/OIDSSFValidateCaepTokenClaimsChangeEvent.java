package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates the required fields for a CAEP Token Claims Change event as defined in
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.2.1">CAEP 1.0 Section 3.2.1</a>:
 * <ul>
 *   <li>{@code claims} - "REQUIRED, JSON object: one or more claims with their new value(s)"</li>
 * </ul>
 * An empty {@code claims} object fails because the spec requires one or more claims.
 * Reads the event payload from {@code ssf.caep_event.data}.
 */
public class OIDSSFValidateCaepTokenClaimsChangeEvent extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		JsonElement claims = eventData.get("claims");
		if (claims == null) {
			throw error("Missing required field 'claims' in token-claims-change event",
				args("event_data", eventData));
		}
		if (!claims.isJsonObject()) {
			throw error("Field 'claims' MUST be a JSON object",
				args("claims", claims, "event_data", eventData));
		}
		if (claims.getAsJsonObject().isEmpty()) {
			throw error("Field 'claims' MUST contain one or more claims with their new value(s)",
				args("claims", claims, "event_data", eventData));
		}

		logSuccess("Token Claims Change event fields are valid", args("event_data", eventData));

		return env;
	}
}
