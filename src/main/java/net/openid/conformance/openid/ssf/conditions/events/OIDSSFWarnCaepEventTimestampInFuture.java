package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Duration;
import java.time.Instant;

/**
 * CAEP 1.0 section 2 defines {@code event_timestamp} as the time at which the event occurred.
 * A value ahead of the receiver's clock, beyond a small allowance for skew, cannot describe an
 * event that has occurred; callers grade it as a WARNING, since the specification sets no
 * bound. Reads {@code ssf.caep_event.data}; a missing or non-numeric value is left to
 * {@link OIDSSFValidateCaepCommonOptionalFields}.
 */
public class OIDSSFWarnCaepEventTimestampInFuture extends AbstractCondition {

	static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(5);

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement el = env.getElementFromObject("ssf", "caep_event.data.event_timestamp");
		if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
			log("No numeric event_timestamp to check");
			return env;
		}

		long timestamp = OIDFJSON.getLong(el);
		Instant eventTime = Instant.ofEpochSecond(Math.min(timestamp, Instant.MAX.getEpochSecond()));
		Instant latestAcceptable = Instant.now().plus(ALLOWED_CLOCK_SKEW);
		if (eventTime.isAfter(latestAcceptable)) {
			throw error("event_timestamp lies in the future; an event timestamp describes when the event occurred",
				args("event_timestamp", timestamp, "event_time", eventTime.toString(), "now", Instant.now().toString(),
					"allowed_clock_skew_seconds", ALLOWED_CLOCK_SKEW.toSeconds()));
		}

		logSuccess("event_timestamp is not in the future", args("event_timestamp", timestamp, "event_time", eventTime.toString()));
		return env;
	}
}
