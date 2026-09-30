package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks the recommendation for {@code change_direction} in a CAEP Assurance Level Change event from
 * <a href="https://openid.net/specs/openid-caep-1_0-final.html#section-3.4.1">CAEP 1.0 Section 3.4.1</a>:
 * "If the Transmitter has specified the previous_level, then the Transmitter SHOULD provide a value
 * for this claim."
 * <p>
 * Throws when {@code previous_level} is present and {@code change_direction} is absent. Callers should
 * invoke this at WARNING severity since the requirement is a SHOULD. Types and permitted values are
 * checked separately by {@link OIDSSFValidateCaepAssuranceLevelChangeEvent}.
 * Reads the event payload from {@code ssf.caep_event.data}.
 */
public class OIDSSFWarnCaepAssuranceLevelChangeDirectionMissing extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		if (!eventData.has("previous_level")) {
			log("previous_level is not present, so change_direction is not expected", args("event_data", eventData));
			return env;
		}

		if (!eventData.has("change_direction")) {
			throw error("previous_level is present but change_direction is missing; "
					+ "the Transmitter SHOULD provide change_direction when it has specified previous_level",
				args("event_data", eventData));
		}

		logSuccess("change_direction is present alongside previous_level", args("event_data", eventData));

		return env;
	}
}
